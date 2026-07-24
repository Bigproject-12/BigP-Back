package com.aivle.bigproject.dto.analysis;

import java.util.List;

/**
 * 대시보드 통계 응답 DTO
 */
public record DashboardResponse(
        long repositoryCount,
        long totalAnalysisCount,
        long analyzingCount,
        long completedCount,
        long failedCount,
        long canceledCount,
        long totalIssueCount,
        long securityIssueCount,
        long inefficiencyIssueCount,
        List<RecentAnalysis> recentAnalyses
){
    public record RecentAnalysis(
            Integer analysisId,
            Integer repoId,
            String repoName,
            String language,
            String status,
            long totalIssueCount
    ) {}
}
