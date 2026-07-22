package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DetectRequest(
    // AI 서버가 'code_content'라는 키값을 찾으므로 @JsonProperty로 맞춰줍니다.
    @JsonProperty("code_content")
    String codeContent,
    Integer repoId,
    Integer companyId,
    Integer userId,
    String language,
    String prompt
) {}
