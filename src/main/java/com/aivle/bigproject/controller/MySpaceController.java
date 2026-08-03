package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.myspace.MySpaceAnalysisResponse;
import com.aivle.bigproject.dto.myspace.MySpaceAnalysisDetailResponse;
import com.aivle.bigproject.dto.myspace.MySpaceSummaryResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResponse;
import com.aivle.bigproject.service.MySpaceService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/my-space")
public class MySpaceController {

    private final MySpaceService mySpaceService;

    public MySpaceController(MySpaceService mySpaceService) {
        this.mySpaceService = mySpaceService;
    }

    @GetMapping("/repos/{repoId}/summary")
    public ResponseEntity<MySpaceSummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch
    ) {
        return ResponseEntity.ok(mySpaceService.getSummary(
                Integer.valueOf(jwt.getSubject()), repoId, branch));
    }

    @GetMapping("/repos/{repoId}/analyses")
    public ResponseEntity<List<MySpaceAnalysisResponse>> getAnalyses(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(mySpaceService.getAnalyses(
                Integer.valueOf(jwt.getSubject()), repoId, branch, page, size));
    }

    @GetMapping("/repos/{repoId}/files/analysis")
    public ResponseEntity<MySpaceAnalysisDetailResponse> getLatestFileAnalysis(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch,
            @RequestParam String path
    ) {
        return ResponseEntity.ok(mySpaceService.getLatestFileAnalysis(
                Integer.valueOf(jwt.getSubject()), repoId, branch, path));
    }

    @GetMapping("/analyses/{analysisId}")
    public ResponseEntity<MySpaceAnalysisDetailResponse> getAnalysisDetail(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer analysisId
    ) {
        return ResponseEntity.ok(mySpaceService.getAnalysisDetail(
                Integer.valueOf(jwt.getSubject()), analysisId));
    }

    @GetMapping("/repos/{repoId}/pull-requests")
    public ResponseEntity<List<GithubPullRequestResponse>> getMyPullRequests(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(mySpaceService.getMyPullRequests(
                Integer.valueOf(jwt.getSubject()), repoId, branch, status, limit));
    }
}
