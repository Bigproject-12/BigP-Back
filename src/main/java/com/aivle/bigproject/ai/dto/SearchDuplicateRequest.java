package com.aivle.bigproject.ai.dto;
/**
 * 저장소 내 중복 코드 검색을 요청하기 위한 DTO.
 */
public record SearchDuplicateRequest(
    Integer repo_id,
    String code_content,
    String language
) {}