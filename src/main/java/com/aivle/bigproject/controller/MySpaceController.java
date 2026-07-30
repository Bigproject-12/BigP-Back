package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.myspace.MySpaceAnalysisResponse;
import com.aivle.bigproject.dto.myspace.MySpaceSummaryResponse;
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
@RequestMapping("/api/my-space/repos")
public class MySpaceController {

    private final MySpaceService mySpaceService;

    public MySpaceController(MySpaceService mySpaceService) {
        this.mySpaceService = mySpaceService;
    }

    @GetMapping("/{repoId}/summary")
    public ResponseEntity<MySpaceSummaryResponse> getSummary(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable Integer repoId,
            @RequestParam String branch
    ) {
        return ResponseEntity.ok(mySpaceService.getSummary(
                Integer.valueOf(jwt.getSubject()), repoId, branch));
    }

    @GetMapping("/{repoId}/analyses")
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
}
