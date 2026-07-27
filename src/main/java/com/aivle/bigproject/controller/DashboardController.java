package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.analysis.DashboardResponse;
import com.aivle.bigproject.service.DashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 대시보드 관련 API를 제공하는 컨트롤러
// 로그인 후 사용자의 대시보드 정보를 조회
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    // 대시보드 로직을 처리
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    //유저가 로그인 후 해당 유저의 대시보드 정보를 조회
    @GetMapping
    public ResponseEntity<DashboardResponse> getDashboard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(
                dashboardService.getDashboard(Integer.valueOf(jwt.getSubject()), from, to)
        );
    }
}
