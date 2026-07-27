package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DetectRequest(
    @JsonProperty("code_content")
    String codeContent,
    Integer repoId,
    String language,
    String prompt,
    String filePath // 히스토리 관련 추가
) {}
