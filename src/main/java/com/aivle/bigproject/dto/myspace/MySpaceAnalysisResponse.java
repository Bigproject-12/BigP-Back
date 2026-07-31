package com.aivle.bigproject.dto.myspace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MySpaceAnalysisResponse(
        Integer analysisId,
        String filePath,
        String branch,
        String status,
        LocalDateTime analyzedAt,
        Integer totalIssueCount,
        Double qualityScore,
        BigDecimal improvableRatio
) {}
