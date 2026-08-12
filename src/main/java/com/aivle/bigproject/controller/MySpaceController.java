package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.myspace.MySpaceAnalysisResponse;
import com.aivle.bigproject.dto.myspace.MySpaceAnalysisDetailResponse;
import com.aivle.bigproject.dto.myspace.MySpaceSummaryResponse;
import com.aivle.bigproject.dto.myspace.MySpaceOverviewResponse;
import com.aivle.bigproject.dto.repo.GithubPullRequestResponse;
import com.aivle.bigproject.service.MySpaceService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 개인 공간(My Space) 관련 API를 제공하는 REST Controller.
 */
@RestController
@RequestMapping("/api/my-space")
public class MySpaceController {

    private final MySpaceService mySpaceService;

    /**
     * MySpaceController 생성자.
     */
    public MySpaceController(MySpaceService mySpaceService) {
        this.mySpaceService = mySpaceService;
    }

     /**
     * 특정 저장소 및 브랜치의 분석 요약 정보를 조회한다.
     *
     * 로그인한 사용자가 접근 가능한 저장소인지 확인한 후,
     * 지정한 브랜치를 기준으로 최신 분석 결과와 주요 지표를 조회
     */
    @GetMapping("/repos/{repoId}/summary")
    public ResponseEntity<MySpaceSummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch
    ) {
        return ResponseEntity.ok(mySpaceService.getSummary(
                Integer.valueOf(jwt.getSubject()), repoId, branch));
    }

    /**
     * 특정 저장소 파일의 최신 분석 결과를 조회
     */
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

    @GetMapping("/overview")
    public ResponseEntity<MySpaceOverviewResponse> getOverview(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(mySpaceService.getOverview(Integer.valueOf(jwt.getSubject()), from, to));
    }
}
