package com.aivle.bigproject.dto.analysis;

import com.aivle.bigproject.entity.Finding;
import java.time.LocalDate;

/**
 * 분석 결과(finding) 목록 조회용 DTO
 * 간단한 카운트 / 메타 정보만 담음 + 대시보드용이랑은 별도
 */

public record FindingSummaryResponse (
        Integer id,
        Integer analysisId,
        boolean isAiGenerated,
        Integer totalIssues,
        Integer securityCount,
        Integer inefficiencyCount,
        LocalDate createdAt
){
    public static FindingSummaryResponse from(Finding finding){
        return new FindingSummaryResponse(
                finding.getId(),
                finding.getAnalysis().getId(),
                finding.isAiGenerated(),
                finding.getTotalIssues(),
                finding.getSecurityCount(),
                finding.getInefficiencyCount(),
                finding.getCreatedAt()
        );
    }
}