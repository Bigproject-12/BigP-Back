package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.service.AnalysisHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

/**
 * 저장소의 코드 분석 이력 조회 API를 제공하는 Controller.
 */
@RestController
@RequestMapping("/api/repos")
public class AnalysisHistoryController {

    //코드 분석 이력 조회 로직을 처리하는 Service
    private final AnalysisHistoryService analysisHistoryService;

    //AnalysisHistoryService를 주입받아 Controller를 생성
    public AnalysisHistoryController(AnalysisHistoryService analysisHistoryService) {
        this.analysisHistoryService = analysisHistoryService;
    }

    // GET /api/repos/{repoId}/history
    //특정 저장소의 코드 분석 이력을 조회
    @GetMapping("/{repoId}/history")
    public ResponseEntity<List<AnalysisHistoryResponse>> getHistory(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId
    ) {
        return ResponseEntity.ok(analysisHistoryService.getHistory(
                Integer.valueOf(jwt.getSubject()), repoId));
    }
}
