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

            if (Boolean.FALSE.equals(redisTemplate.hasKey(historyKey))) {
                String bornAtStr = redisTemplate.opsForValue().get(bornKey);
                long age = (bornAtStr != null) ? (now - Long.parseLong(bornAtStr)) : 99999;

                if (age > 10000) {
                    redisTemplate.opsForSet().remove("active_workers", port);
                    redisTemplate.delete(bornKey);
                    redisTemplate.delete("worker:" + port + ":score");
                } else {
                    redisTemplate.opsForValue().set("worker:" + port + ":score", "100.0");
                }
                continue;
            }

            List<String> history = redisTemplate.opsForList().range(historyKey, 0, -1);
            double score = calculateWeightedScore(history);
            redisTemplate.opsForValue().set("worker:" + port + ":score", String.valueOf(score));
        }
    }

    private double calculateWeightedScore(List<String> history) {
        if (history == null || history.isEmpty()) {
            return 100.0;
        }

        try {
            double totalCpu = 0;
            double networkDelay = 0;
            int count = history.size();

            JsonNode latestNode = mapper.readTree(history.get(0));
            if (latestNode.has("networkDelay")) {
                networkDelay = latestNode.get("networkDelay").asDouble();
            } else if (latestNode.has("baseDelay")) {
                networkDelay = latestNode.get("baseDelay").asDouble();
            }

            for (String jsonStr : history) {
                JsonNode node = mapper.readTree(jsonStr);
                totalCpu += node.get("cpu").asDouble();
            }

            double avgCpu = totalCpu / count;
            double availabilityPenalty = (5 - count) * 10.0;
            double delayPenalty = networkDelay / 10.0;

            return avgCpu + delayPenalty + availabilityPenalty;
        } catch (JacksonException e) {
            return 100.0;
        }
    }
}
