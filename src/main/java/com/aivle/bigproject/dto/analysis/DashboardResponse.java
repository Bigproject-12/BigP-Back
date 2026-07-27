package com.aivle.bigproject.dto.analysis;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        double averageQualityScore,
        Comparison comparison,
        List<QualityTrend> qualityScoreTrend,
        List<IssueDistribution> issueDistribution,
        List<RiskRepository> riskRepositories,
        List<RecentAnalysis> recentAnalyses
){
    public record Comparison(
            double analysisChangeRate,
            double issueChangeRate,
            double qualityScoreChange
    ) {}

    public record QualityTrend(
            LocalDate date,
            Double averageScore
    ) {}

    public record IssueDistribution(
            String type,
            long count,
            double percentage
    ) {}

    public record RiskRepository(
            int rank,
            Integer repoId,
            String repoName,
            double qualityScore,
            long totalIssueCount,
            long securityIssueCount,
            long inefficiencyIssueCount,
            long otherIssueCount,
            LocalDateTime lastAnalyzedAt
    ) {}

    public record RecentAnalysis(
            Integer analysisId,
            Integer repoId,
            String repoName,
            String language,
            String status,
            long totalIssueCount
    ) {}
}
