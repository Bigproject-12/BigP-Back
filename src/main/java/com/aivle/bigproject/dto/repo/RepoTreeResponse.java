package com.aivle.bigproject.dto.repo;

import java.util.List;
import java.time.LocalDateTime;

public record RepoTreeResponse(
        Integer repoId,
        String branch,
        boolean truncated,
        List<Item> items
) {
    public record Item(
            String path,
            String type,
            String sha,
            Long size,
            int totalIssueCount,
            int securityIssueCount,
            int inefficiencyIssueCount,
            int otherIssueCount,
            boolean analyzed,
            Integer analysisId,
            LocalDateTime lastAnalyzedAt,
            Double qualityScore,
            int criticalIssueCount,
            int highIssueCount,
            int mediumIssueCount,
            int lowIssueCount
    ) {}
}
