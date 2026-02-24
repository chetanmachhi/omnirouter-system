package com.omnirouter.brain.controller;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    public String dispatchTask(@RequestBody String body) {
        int maxRetries = 3;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            String bestPort = findBestWorker();

            if (bestPort == null) {
                return "Error: No active workers found in the cluster.";
            }

            try {
                System.out.println("[DISPATCH] 🧠 Attempt " + attempt + ": Routing to Port " + bestPort);

                return restClient.post()
                        .uri("http://localhost:" + bestPort + "/execute")
                        .body(body)
                        .retrieve()
                        .body(String.class);

            } catch (Exception e) {
                System.err.println("[DISPATCH] ❌ Port " + bestPort + " failed. Applying penalty and retrying...");

                redisTemplate.opsForValue().set("worker:" + bestPort + ":score", "999.0");
            }
        }

        return "Error: All candidate workers failed after " + maxRetries + " attempts.";
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
