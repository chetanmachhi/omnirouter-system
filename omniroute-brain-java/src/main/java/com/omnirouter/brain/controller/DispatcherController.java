package com.omnirouter.brain.controller;

import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

@RestController
@RequestMapping("/api/dispatch")
public class DispatcherController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.create();

    @PostMapping("/execute")
    public Map<String, Object> dispatchTask(@RequestBody String body) {
        Map<String, Object> result = new HashMap<>();

        // 1. Instantly pick the best worker
        String bestPort = findBestWorker();
        Map<String, String> snapshot = getActiveWorkers();

        if (bestPort == null) {
            result.put("error", "No active workers");
            return result;
        }

        // 2. Fire the request in a background thread (Don't wait!)
        CompletableFuture.runAsync(() -> {
            try {
                restClient.post()
                        .uri("http://localhost:" + bestPort + "/execute")
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
            } catch (Exception e) {
                redisTemplate.opsForValue().set("worker:" + bestPort + ":score", "999.0");
            }
        });

        // 3. Return the "Server Selection" data immediately
        result.put("port", bestPort);
        result.put("timestamp", new Date().getTime());
        result.put("snapshot", snapshot);
        result.put("status", "Task Dispatched");

        return result;
    }

    private String findBestWorker() {
        Set<String> activePorts = redisTemplate.opsForSet().members("active_workers");
        if (activePorts == null || activePorts.isEmpty()) {
            return null;
        }

        return activePorts.stream()
                .min(Comparator.comparingDouble(port -> {
                    String scoreStr = redisTemplate.opsForValue().get("worker:" + port + ":score");
                    return (scoreStr != null) ? Double.valueOf(scoreStr) : 100.0;
                }))
                .orElse(null);
    }

    @GetMapping("/active-workers")
    public Map<String, String> getActiveWorkers() {
        Set<String> ports = redisTemplate.opsForSet().members("active_workers");
        Map<String, String> response = new HashMap<>();

        if (ports != null) {
            for (String port : ports) {
                String score = redisTemplate.opsForValue().get("worker:" + port + ":score");
                response.put(port, score != null ? score : "Calculating...");
            }
        }
        return response;
    }
}
