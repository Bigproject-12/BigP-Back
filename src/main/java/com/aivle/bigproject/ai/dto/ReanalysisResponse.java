package com.aivle.bigproject.ai.dto;

/**
 * AI 코드 재분석 요청 결과를 반환하기 위한 응답 DTO.
 */
public record ReanalysisResponse(
        Integer analysisId,
        String status
) {}
