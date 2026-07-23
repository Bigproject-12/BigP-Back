package com.aivle.bigproject.dto.analysis;

import com.aivle.bigproject.entity.Finding;
import java.time.LocalDate;

/**
 * 분석 결과 (Finding) 조회 응답 DTO
 * Finding 엔티티 구조를 그대로 반영
 * -> analysis는 analysisId로 Id만 추출하게
 */
public record FindingResponse(
        Integer id,             // finding의 pk
        Integer analysisId,     // 어떤 분석 결과인지 id값만 노출

        boolean isAiGenerated,

        //=== 집계 ===
        Integer totalIssues,
        Integer securityCount,
        Integer inefficiencyCount,

        //=== 분석 결과 본문 ===
        String inefficiencyResult,
        String secuResult,
        String duplicateResult,
        String modifiedCode,

        LocalDate createdAt     // ← 오타 수정
) {
    public static FindingResponse from(Finding finding) {
        return new FindingResponse(
                finding.getId(),
                finding.getAnalysis().getId(),
                finding.isAiGenerated(),
                finding.getTotalIssues(),
                finding.getSecurityCount(),
                finding.getInefficiencyCount(),
                finding.getInefficiencyResult(),
                finding.getSecuResult(),
                finding.getDuplicateResult(),
                finding.getModifiedCode(),
                finding.getCreatedAt()
        );
    }
}