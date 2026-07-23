package com.aivle.bigproject.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;

    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/health")
    public String health() {
        return "BigP-Back server is running";
    }

    @GetMapping("/health/db")
    public Map<String, String> databaseHealth() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return Map.of(
                "database",
                result != null && result == 1 ? "UP" : "DOWN"
        );
    }
}