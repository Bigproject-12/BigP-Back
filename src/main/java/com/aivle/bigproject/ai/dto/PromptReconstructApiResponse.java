package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonAlias;

public record PromptReconstructApiResponse(
    @JsonAlias("reconstructed_prompt") String reconstructedPrompt,
    String explanation
) {}