package com.aivle.bigproject.ai.controller;

import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.DetectResponse;
import com.aivle.bigproject.ai.dto.AnalysisResultResponse;
import com.aivle.bigproject.ai.service.AnalysisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController { 
    
    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    /**
     * 프론트엔드에서 분석 요청을 받아서 AI 서버로 전달
     * POST /api/analysis/detect
     */
    @PostMapping
    public ResponseEntity<DetectResponse> requestAnalysis(@RequestBody DetectRequest request, @AuthenticationPrincipal Jwt jwt) {

        Integer loggedInUserId = Integer.valueOf(jwt.getSubject());

        DetectResponse aiResult = analysisService.sendToAiServer(request, loggedInUserId);
        
        // AI 서버의 결과를 다시 프론트엔드로 반환
        return ResponseEntity.ok(aiResult);
    }

    @GetMapping("/{analysis_id}")
    public ResponseEntity<AnalysisResultResponse> getAnalysisResult(@PathVariable("analysis_id") Integer analysisId) {
        AnalysisResultResponse response = analysisService.getAnalysisResult(analysisId);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{analysis_id}")
    public ResponseEntity<String> stopAnalysis(@PathVariable("analysis_id") Integer analysisId) {
        analysisService.stopAnalysis(analysisId);
        return ResponseEntity.ok("분석이 중지되었습니다.");
    }
}
