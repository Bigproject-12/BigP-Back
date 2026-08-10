package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

/**
 * AI 프롬프트 재구성 결과를 전달받기 위한 응답 DTO.
 */
public record PromptReconstructApiResponse(
    @JsonAlias("reconstructed_prompt") String reconstructedPrompt,
    String explanation
) {}