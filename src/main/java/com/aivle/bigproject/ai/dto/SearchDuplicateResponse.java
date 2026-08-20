package com.aivle.bigproject.ai.dto;

import java.util.List;

/**
 * 저장소 내 중복 코드 검색 결과를 전달받기 위한 응답 DTO.
 */
public record SearchDuplicateResponse(
    List<VectorMatch> duplicates
) {}