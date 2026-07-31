package com.aivle.bigproject.dto.myspace;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record MySpaceAnalysisDetailResponse(
        Integer analysisId,
        Integer repoId,
        String repoName,
        String branch,
        String filePath,
        String language,
        String status,
        LocalDateTime analyzedAt,
        int totalIssueCount,
        int securityIssueCount,
        int inefficiencyIssueCount,
        int otherIssueCount,
        double qualityScore,
        BigDecimal improvableRatio,
        List<Issue> securityIssues,
        List<Issue> inefficiencyIssues,
        List<Issue> otherIssues,
        String originCode,
        String modifiedCode,
        boolean aiGenerated,
        Double aiProbability
) {
    public record Issue(
            String type,
            String severity,
            Integer line,
            String ruleId,
            String functionName,
            String description,
            String suggestion,
            Double score
    ) {
    }
}
