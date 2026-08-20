package com.aivle.bigproject.dto.myspace;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * My Space의 저장소별 코드 분석 요약 정보를 반환하기 위한 응답 DTO.
 *
 * 특정 저장소의 최신 분석 결과를 기준으로 분석 횟수, 발견된 이슈,
 * 개선 가능 비율, 코드 품질 점수 등의 주요 정보를 제공한다.
 */
public record MySpaceSummaryResponse(
        Integer repoId,
        String repoName,
        String branch,
        Integer analysisId,
        LocalDateTime lastAnalyzedAt,
        String filePath,
        long totalAnalysisCount,
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
    /**
     * 이전 분석 결과 대비 주요 코드 분석 지표의 변화량을 표현하는 DTO.
     */
    public record Comparison(
            Integer totalIssueChange,
            Integer securityIssueChange,
            Integer inefficiencyIssueChange,
            BigDecimal improvableRatioChange,
            Double qualityScoreChange
    ) {}
}
