package com.omnirouter.brain.service;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class HealthMonitorService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper mapper = new ObjectMapper();

    @Scheduled(fixedRate = 1000)
    public void monitor() {
        Set<String> activePorts = redisTemplate.opsForSet().members("active_workers");

        if (activePorts == null || activePorts.isEmpty()) {
            return;
        }

        for (String portStr : activePorts) {
            int port = Integer.parseInt(portStr);

            CompletableFuture.runAsync(() -> {
                try {
                    String statsJson = restClient.get()
                            .uri("http://localhost:" + port + "/health")
                            .retrieve()
                            .body(String.class);

                    if (statsJson != null) {
                        String historyKey = "worker:" + port + ":history";
                        String scoreKey = "worker:" + port + ":score";

                        redisTemplate.opsForList().leftPush(historyKey, statsJson);
                        redisTemplate.opsForList().trim(historyKey, 0, 4);
                        redisTemplate.expire(historyKey, Duration.ofSeconds(10));

                        String currentScore = redisTemplate.opsForValue().get(scoreKey);
                        JsonNode json = mapper.readTree(statsJson);
                        String cpu = json.get("cpu").asText();
                        String delay = json.has("networkDelay") ? json.get("networkDelay").asText()
                                : json.has("baseDelay") ? json.get("baseDelay").asText() : "0";

                        System.out.printf("[HEALTH] Worker %d | CPU: %s%% | Delay: %sms | Score: %s | History: %d/5%n",
                                port, cpu, delay, currentScore != null ? currentScore : "N/A",
                                redisTemplate.opsForList().size(historyKey));
                    }
                } catch (JacksonException e) {
                    System.err.println("[OFFLINE] Alert: Worker " + port + " is not responding!");
                }
            });
        }
    }
}
