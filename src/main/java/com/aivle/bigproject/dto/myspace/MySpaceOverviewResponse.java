package com.aivle.bigproject.dto.myspace;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * My Space 대시보드의 코드 분석 개요 정보를 반환하기 위한 응답 DTO.
 * 사용자의 저장소 및 코드 분석 현환을 기반으로 정보 제공 
 * 저장소 및 분석 통계, 품질 점수 추세, 이슈 분포, 위험 저장소, 최근 분석 및 최근 PR 정보 등을 포함
 */
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
     /**
      * 이전 기간 대비 주요 분석 지표의 변화 정보를 표현하는 DTO.
      */
    public record Comparison(
            double analysisChangeRate,
            double issueChangeRate,
            double qualityScoreChange
    ) {}

        /**
         * 날짜별 평균 코드 품질 점수 정보를 표현하는 DTO.
         */
    public record QualityTrend(
            LocalDate date,
            Double averageScore
    ) {}

        /**
         * 이슈 유형별 분포 정보를 표현하는 DTO.
         */
    public record IssueDistribution(
            String type,
            long count,
            double percentage
    ) {}

        /**
         * 품질 위험도가 높은 저장소 정보를 표현하는 DTO.
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
         * 최근 코드 분석 정보를 표현하는 DTO.
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
         * 최근 Pull Request 정보를 표현하는 DTO.
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
