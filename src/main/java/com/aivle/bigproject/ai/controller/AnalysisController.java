package com.aivle.bigproject.ai.controller;

import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.AnalysisResultResponse;
import com.aivle.bigproject.dto.repo.PullRequestCreate;
import com.aivle.bigproject.ai.service.AnalysisService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


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
    public ResponseEntity<Map<String, Integer>> requestAnalysis(@RequestBody DetectRequest request, @AuthenticationPrincipal Jwt jwt) {

        Integer loggedInUserId = Integer.valueOf(jwt.getSubject());

        Integer analysisId = analysisService.createInitialAnalysis(request, loggedInUserId);

        analysisService.sendToAiServerAsync(analysisId, request);

        return ResponseEntity.ok(Map.of("analysis_id", analysisId));
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

    @PostMapping("/{analysis_id}/push")
    public ResponseEntity<Void> pushToGithub(
            @PathVariable("analysis_id") Integer analysisId,
            @AuthenticationPrincipal Jwt jwt
    ) { analysisService.pushImprovedCode(analysisId, Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{analysis_id}/pr")
    public ResponseEntity<Map<String,String>> createPullRequest(
            @PathVariable("analysis_id") Integer analysisId,
            @RequestBody(required = false) PullRequestCreate request,
            @AuthenticationPrincipal Jwt jwt
    ) { String baseBranch = request != null ? request.baseBranch() : null;
        String prUrl = analysisService.createPullRequest(analysisId, Integer.valueOf(jwt.getSubject()), baseBranch);
        return ResponseEntity.ok(Map.of("prUrl", prUrl));
    }
}