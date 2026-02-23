package com.omnirouter.brain.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registry")
public class RegistrationController {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @PostMapping("/register")
    public String registerWorker(@RequestParam(name = "port") int port) {
        String portStr = String.valueOf(port);
        redisTemplate.opsForSet().add("active_workers", portStr);
        redisTemplate.opsForValue().set("worker:" + portStr + ":born", String.valueOf(System.currentTimeMillis()));

        System.out.println("[REGISTRY] Worker on port " + port + " registered.");
        return "Registered";
    }

    @PostMapping("/unregister")
    public void unregisterWorker(@RequestParam(name = "port") int port) {
        // Removes from the monitoring list and cleans up history
        redisTemplate.opsForSet().remove("active_workers", String.valueOf(port));
        redisTemplate.delete("worker:" + port + ":history");
        System.out.println("[REGISTRY] ❌ Worker on port " + port + " has been removed.");
    }
}
