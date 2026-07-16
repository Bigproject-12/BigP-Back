package com.aivle.bigproject.dto.analysis;

import java.util.List;

/**
 * 대시보드 통계 응답 DTO
 */
public record DashboardResponse(
        long monthlyAnalysisCount,
        long vulnerabilityCount,
        double avgImprovementRate,
        long autoPrCount,
        List<QualityTrendPoint> qualityTrend,
        List<IssueTypeBucket> issueTypeDistribution,
        String styleSummary   // 스타일요약 - myspace
){
    public record QualityTrendPoint(String label, double score) {}
    public record IssueTypeBucket(String issueType, long count) {}
}