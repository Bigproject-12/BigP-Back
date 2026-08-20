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
        long totalPullRequestCount,
        long openPullRequestCount,
        long mergedPullRequestCount,
        long closedPullRequestCount,
        Comparison comparison,
        List<QualityTrend> qualityScoreTrend,
        List<IssueDistribution> issueDistribution,
        List<RiskRepository> riskRepositories,
        List<RecentAnalysis> recentAnalyses,
        List<RecentPullRequest> recentPullRequests
){
     /**
     * 이전 기간 대비 주요 지표 변화 정보.
     */
    public record Comparison(
            double analysisChangeRate,
            double issueChangeRate,
            double qualityScoreChange,
            double pullRequestChangeRate
    ) {}
     /**
     * 날짜별 평균 코드 품질 점수 정보.
     */
    public record QualityTrend(
            LocalDate date,
            Double averageScore
    ) {}

     /**
     * 이슈 유형별 분포 정보.
     */
    public record IssueDistribution(
            String type,
            long count,
            double percentage
    ) {}
     /**
     * 품질 위험도가 높은 저장소 정보.
     */
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
     /**
     * 최근 코드 분석 정보.
     */
    public record RecentAnalysis(
            Integer analysisId,
            Integer repoId,
            String repoName,
            String language,
            String status,
            long totalIssueCount
    ) {}
    /**
     * 최근 Pull Request 정보.
     */
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
