package com.aivle.bigproject.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DetectRequest(
    // AI 서버가 'code_content'라는 키값을 찾으므로 @JsonProperty로 맞춰줍니다.
    @JsonProperty("code_content")
    String codeContent
) {}