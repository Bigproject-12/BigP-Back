package com.aivle.bigproject.ai.dto;


/**
 * AI 코드 재분석 시작에 필요한 정보를 전달하는 DTO.
 *
 * 기존 분석 데이터의 ID와 AI 서버에 전달할 분석 요청 정보를 함께 저장하여
 * 코드 재분석 작업을 시작할 때 사용한다.
 */
public record ReanalysisStart(
        Integer analysisId,
        DetectRequest request
) {}
