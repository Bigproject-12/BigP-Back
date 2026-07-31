package com.aivle.bigproject.dto.myspace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MySpaceSummaryResponse(
        Integer repoId,
        String repoName,
        String branch,
        Integer analysisId,
        LocalDateTime lastAnalyzedAt,
        String filePath,
        int totalIssueCount,
        int securityIssueCount,
        int inefficiencyIssueCount,
        int otherIssueCount,
        BigDecimal improvableRatio,
        double qualityScore,
        Comparison comparison,
        String securityResult,
        String inefficiencyResult,
        String originCode,
        String modifiedCode
) {
    public record Comparison(
            Integer totalIssueChange,
            Integer securityIssueChange,
            Integer inefficiencyIssueChange,
            BigDecimal improvableRatioChange,
            Double qualityScoreChange
    ) {}
}
