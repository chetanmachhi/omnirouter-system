package com.omnirouter.brain.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;

@Service
public class HealthMonitorService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.create();

    @Scheduled(fixedRate = 5000)
    public void monitor() {
        try {
            String stats = restClient.get()
                .uri("http://localhost:4001/health")
                .retrieve()
                .body(String.class);

            redisTemplate.opsForValue().set("worker:4001:stats", stats, Duration.ofSeconds(10));
            System.out.println("💓 Heartbeat: Worker 4001 is Healthy");
        } catch (Exception e) {
            System.err.println("⚠️ Alert: Worker 4001 is Offline!");
        }
    }
}