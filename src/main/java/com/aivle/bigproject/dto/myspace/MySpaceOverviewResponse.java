package com.aivle.bigproject.dto.myspace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MySpaceOverviewResponse(
        long repositoryCount,
        long totalAnalysisCount,
        long totalIssueCount,
        double averageQualityScore,
        Comparison comparison,
        List<QualityTrend> qualityScoreTrend,
        List<IssueDistribution> issueDistribution,
        List<RiskRepository> riskRepositories,
        List<RecentAnalysis> recentAnalyses,
        List<RecentPullRequest> recentPullRequests
) {
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

    public record RecentPullRequest(
            Integer pullRequestId,
            Integer githubPrNumber,
            Integer repoId,
            String repoName,
            String title,
            String status,
            String prUrl,
            String headBranch,
            String baseBranch,
            LocalDateTime createdAt
    ) {}
}
