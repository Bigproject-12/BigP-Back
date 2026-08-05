package com.aivle.bigproject.ai.controller;

import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.PromptReconstructApiResponse;
import com.aivle.bigproject.ai.dto.ReconstructPromptRequest;
import com.aivle.bigproject.ai.dto.AnalysisResultResponse;
import com.aivle.bigproject.ai.dto.ReanalysisStart;
import com.aivle.bigproject.ai.dto.ReanalysisResponse;
import com.aivle.bigproject.dto.repo.PullRequestCreate;
import com.aivle.bigproject.dto.analysis.BatchPushResponse;
import com.aivle.bigproject.dto.analysis.BatchPushRequest;
import com.aivle.bigproject.dto.analysis.BatchPullRequestRequest;
import com.aivle.bigproject.dto.analysis.BatchPullRequestResponse;
import com.aivle.bigproject.ai.service.AnalysisService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Map;
import java.util.List;


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
        System.out.println("[TRACE] 컨트롤러 진입, request.repoId()=" + request.repoId());
        Integer loggedInUserId = Integer.valueOf(jwt.getSubject());

        Integer analysisId = analysisService.createInitialAnalysis(request, loggedInUserId);

        analysisService.sendToAiServerAsync(analysisId, request);

        return ResponseEntity.ok(Map.of("analysis_id", analysisId));
    }

    @GetMapping("/{analysis_id}")
    public ResponseEntity<AnalysisResultResponse> getAnalysisResult(
            @PathVariable("analysis_id") Integer analysisId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        AnalysisResultResponse response = analysisService.getAnalysisResult(
                analysisId, Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{analysis_id}")
    public ResponseEntity<String> stopAnalysis(
            @PathVariable("analysis_id") Integer analysisId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        analysisService.stopAnalysis(analysisId, Integer.valueOf(jwt.getSubject()));
        return ResponseEntity.ok("분석이 중지되었습니다.");
    }

    @PostMapping("/{analysis_id}/push")
    public ResponseEntity<Void> pushToGithub(
            @PathVariable("analysis_id") Integer analysisId,
            @RequestParam(defaultValue = "false") boolean overwriteChangedFiles,
            @AuthenticationPrincipal Jwt jwt
    ) { analysisService.pushImprovedCode(
            analysisId, Integer.valueOf(jwt.getSubject()), overwriteChangedFiles);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/batch-push")
    public ResponseEntity<BatchPushResponse> batchPush(
            @Valid @RequestBody BatchPushRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(analysisService.batchPush(
                request, Integer.valueOf(jwt.getSubject())));
    }

    @PostMapping("/batch-pull-request")
    public ResponseEntity<BatchPullRequestResponse> createBatchPullRequest(
            @Valid @RequestBody BatchPullRequestRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(analysisService.createBatchPullRequest(
                request, Integer.valueOf(jwt.getSubject())));
    }

    @PostMapping("/{analysis_id}/reanalyze")
    public ResponseEntity<ReanalysisResponse> reanalyze(
            @PathVariable("analysis_id") Integer analysisId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        ReanalysisStart reanalysis = analysisService.prepareReanalysis(
                analysisId, Integer.valueOf(jwt.getSubject()));
        analysisService.sendToAiServerAsync(reanalysis.analysisId(), reanalysis.request());
        return ResponseEntity.accepted().body(
                new ReanalysisResponse(reanalysis.analysisId(), "ANALYZING"));
    }

    @PostMapping("/{analysis_id}/pr")
    public ResponseEntity<Map<String,String>> createPullRequest(
            @PathVariable("analysis_id") Integer analysisId,
            @RequestBody(required = false) PullRequestCreate request,
            @AuthenticationPrincipal Jwt jwt
    ) { String baseBranch = request != null ? request.baseBranch() : null;
        String title      = request != null ? request.title()      : null;
        String body       = request != null ? request.body()       : null;

        String prUrl = analysisService.createPullRequest(analysisId, Integer.valueOf(jwt.getSubject()), baseBranch,title,body);
        return ResponseEntity.ok(Map.of("prUrl", prUrl));
    }

    @PostMapping("/{analysisId}/reconstruct-prompt")
    public ResponseEntity<PromptReconstructApiResponse> reconstructPrompt(
            @PathVariable Integer analysisId,
            @RequestBody ReconstructPromptRequest request
    ) {
        return ResponseEntity.ok(analysisService.reconstructPrompt(analysisId, request.originalPrompt()));
    }
}
