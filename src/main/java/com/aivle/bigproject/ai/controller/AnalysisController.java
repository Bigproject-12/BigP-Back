package com.aivle.bigproject.aivle.bigproject.ai.controller;

import com.aivle.bigproject.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.aivle.bigproject.ai.dto.DetectResponse;
import com.aivle.bigproject.aivle.bigproject.ai.service.AnalysisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<DetectResponse> requestAnalysis(@RequestBody DetectRequest request) {
        
        // 서비스 계층을 통해 FastAPI 서버로 통신
        DetectResponse aiResult = analysisService.sendToAiServer(request);
        
        // AI 서버의 결과를 다시 프론트엔드로 반환
        return ResponseEntity.ok(aiResult);
    }
}