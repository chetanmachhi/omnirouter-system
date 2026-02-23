package com.omnirouter.brain.service;
import java.time.Duration;
import java.util.List;

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
    private final List<Integer> workerPorts = List.of(4001, 4002, 4003);

    @Scheduled(fixedRate = 5000)
    public void monitor() {
        // .parallelStream() handles the multi-threading for you
        workerPorts.parallelStream().forEach(port -> {
            try {
                String statsJson = restClient.get()
                        .uri("http://localhost:" + port + "/health")
                        .retrieve()
                        .body(String.class);

                if (statsJson != null) {
                    redisTemplate.opsForValue().set("worker:" + port + ":stats", statsJson, Duration.ofSeconds(10));

                    JsonNode json = mapper.readTree(statsJson);
                    String cpu = json.get("cpu").asText();
                    String mem = json.get("mem").asText();
                    String delay = json.has("baseDelay") ? json.get("baseDelay").asText() : "0";

                    System.out.printf("💓 Worker %d | CPU: %s%% | RAM Free: %sMB | Delay: %sms%n",
                            port, cpu, mem, delay);
                }
            } catch (JacksonException e) {
                System.err.println("⚠️ Alert: Worker " + port + " is Offline!");
            }
        });
    }
}
