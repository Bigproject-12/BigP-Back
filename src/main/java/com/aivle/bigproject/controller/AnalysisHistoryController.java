package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.service.AnalysisHistoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repos")
public class AnalysisHistoryController {

    private final AnalysisHistoryService analysisHistoryService;

    public AnalysisHistoryController(AnalysisHistoryService analysisHistoryService) {
        this.analysisHistoryService = analysisHistoryService;
    }

    // GET /api/repos/{repoId}/history
    @GetMapping("/{repoId}/history")
    public ResponseEntity<List<AnalysisHistoryResponse>> getHistory(@PathVariable Integer repoId) {
        return ResponseEntity.ok(analysisHistoryService.getHistory(repoId));
    }
}