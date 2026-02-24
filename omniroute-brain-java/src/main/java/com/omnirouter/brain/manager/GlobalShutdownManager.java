package com.omnirouter.brain.manager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

@Component
public class GlobalShutdownManager {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Value("${MASTER_URL:http://localhost:4000}")
    private String masterUrl;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SYSTEM] SIGINT Detected. Cleaning up...");
            try {
                restClient.delete()
                        .uri(masterUrl + "/kill-all")
                        .retrieve()
                        .toBodilessEntity();

                redisTemplate.delete("active_workers");
                System.out.println("[SYSTEM] Cleanup complete.");
            } catch (Exception e) {
                System.err.println("[SYSTEM] Cleanup failed: " + e.getMessage());
            }
        }));
    }
}
