package com.aivle.bigproject.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
// 해당 java 의 경우 서버가 정상적으로 실행중인지 체크 하기 위한 api//
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "BigP-Back server is running";
    }
}