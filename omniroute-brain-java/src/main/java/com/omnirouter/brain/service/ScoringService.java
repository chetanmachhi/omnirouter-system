package com.omnirouter.brain.service;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class ScoringService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Scheduled(fixedRate = 1000)
    public void calculateScores() {
        Set<String> activePorts = redisTemplate.opsForSet().members("active_workers");
        if (activePorts == null) {
            return;
        }

        long now = System.currentTimeMillis();

        for (String port : activePorts) {
            String historyKey = "worker:" + port + ":history";
            String bornKey = "worker:" + port + ":born";

            Boolean hasHistory = redisTemplate.hasKey(historyKey);

            if (Boolean.FALSE.equals(hasHistory)) {
                String bornAtStr = redisTemplate.opsForValue().get(bornKey);
                long age = (bornAtStr != null) ? (now - Long.parseLong(bornAtStr)) : 99999;

                if (age > 10000) {
                    System.out.println("[REAPER] Worker " + port + " pulse lost. Removing.");
                    redisTemplate.opsForSet().remove("active_workers", port);
                    redisTemplate.delete(bornKey);
                    redisTemplate.delete("worker:" + port + ":score");
                } else {
                    redisTemplate.opsForValue().set("worker:" + port + ":score", "100.0");
                }
                continue;
            }

            List<String> history = redisTemplate.opsForList().range(historyKey, 0, -1);
            double score = calculatePenalty(history);
            redisTemplate.opsForValue().set("worker:" + port + ":score", String.valueOf(score));
        }
    }

    private double calculatePenalty(List<String> history) {
        if (history == null || history.isEmpty()) {
            return 100.0;
        }

        try {
            double totalCpu = 0;
            int count = history.size();

            for (String jsonStr : history) {
                JsonNode node = mapper.readTree(jsonStr);
                totalCpu += node.get("cpu").asDouble();
            }

            double avgCpu = totalCpu / count;

            double availabilityPenalty = (5 - count) * 20.0;

            return Math.min(100.0, availabilityPenalty + (avgCpu * 0.9));
        } catch (JacksonException e) {
            return 100.0;
        }
    }
}
