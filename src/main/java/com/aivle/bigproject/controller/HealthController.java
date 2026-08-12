package com.aivle.bigproject.controller;

import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 및 데이터베이스 상태 확인 API를 제공하는 REST Controller.
 */
@RestController
@RequestMapping("/api")
public class HealthController {


    private final JdbcTemplate jdbcTemplate;

    /**
     * HealthController 생성자.
     */
    public HealthController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 서버 상태를 확인하는 API.
     *
     * @return 서버 상태 메시지
     */
    @GetMapping("/health")
    public String health() {
        return "BigP-Back server is running";
    }

    /**
     * 데이터베이스 연결 상태를 확인
     *
     * 데이터베이스에 간단한 SELECT 쿼리를 실행하여
     * 정상적으로 결과를 반환하는지 확인
     * @return 데이터베이스 상태 메시지
     */
    @GetMapping("/health/db")
    public Map<String, String> databaseHealth() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        return Map.of(
                "database",
                result != null && result == 1 ? "UP" : "DOWN"
        );
    }
}