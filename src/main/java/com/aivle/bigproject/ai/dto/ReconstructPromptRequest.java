/**
 * AI 프롬프트 재구성을 요청하기 위한 DTO.
 */
package com.aivle.bigproject.ai.dto;

public record ReconstructPromptRequest(
    String originalPrompt
) {}