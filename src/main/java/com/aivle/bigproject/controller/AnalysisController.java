package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.analysis.AnalysisRequest;
import com.aivle.bigproject.dto.analysis.AnalysisResponse;
import com.aivle.bigproject.service.AnalysisService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analyses")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public AnalysisResponse analyze(
            @Valid @RequestBody AnalysisRequest request
    ) {
        return analysisService.analyze(request);
    }
}