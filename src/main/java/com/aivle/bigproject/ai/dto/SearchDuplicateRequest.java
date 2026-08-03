package com.aivle.bigproject.ai.dto;

public record SearchDuplicateRequest(
    Integer repo_id,
    String code_content,
    String language
) {}