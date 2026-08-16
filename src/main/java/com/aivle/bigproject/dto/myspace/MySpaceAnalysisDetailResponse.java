package com.aivle.bigproject.dto.myspace;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


/**
 * My Space의 코드 분석 상세 정보를 반환하기 위한 응답 DTO.
 *
 * 특정 분석 결과에 대한 저장소 및 파일 정보와 함께
 * 보안 취약점, 비효율성, 기타 이슈 등의 분석 결과를 제공
 */
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

     /**
     * 코드 분석 과정에서 발견된 이슈의 상세 정보를 표현하는 DTO.
     */
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
