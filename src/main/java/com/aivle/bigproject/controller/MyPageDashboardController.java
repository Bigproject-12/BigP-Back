package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.analysis.DashboardResponse;
import com.aivle.bigproject.service.MyPageDashboardService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage")
public class MyPageDashboardController {

    private final MyPageDashboardService myPageDashboardService;

    public MyPageDashboardController(MyPageDashboardService myPageDashboardService) {
        this.myPageDashboardService = myPageDashboardService;
    }

    /**
     * 마이페이지(MySpace)에서 특정 레포지토리별 대시보드 통계 조회
     * 프론트엔드 요청 예시: GET /api/mypage/repos/123/dashboard?from=2026-06-01&to=2026-06-07
     */
    @GetMapping("/repos/{repoId}/dashboard")
    public ResponseEntity<DashboardResponse> getRepoDashboard(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        // JWT 토큰에서 로그인한 유저의 ID 추출
        Integer userId = Integer.valueOf(jwt.getSubject());
        
        DashboardResponse response = myPageDashboardService.getRepoDashboard(userId, repoId, from, to);
        return ResponseEntity.ok(response);
    }
}