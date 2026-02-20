package com.omnirouter.brain.service;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HealthMonitorService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.create();
    private final List<Integer> workerPorts = List.of(4001, 4002, 4003);

    @Scheduled(fixedRate = 5000)
    public void monitor() {
        // .parallelStream() handles the multi-threading for you
        workerPorts.parallelStream().forEach(port -> {
            try {
                String stats = restClient.get()
                        .uri("http://localhost:" + port + "/health")
                        .retrieve()
                        .body(String.class);

                redisTemplate.opsForValue().set("worker:" + port + ":stats", stats, Duration.ofSeconds(10));
                System.out.println("💓 Simultaneous Heartbeat: Worker " + port + " is Healthy");
            } catch (Exception e) {
                System.err.println("⚠️ Alert: Worker " + port + " is Offline!");
            }
        });
    }
}
