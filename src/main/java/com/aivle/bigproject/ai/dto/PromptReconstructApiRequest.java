package com.aivle.bigproject.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PromptReconstructApiRequest(
    @JsonProperty("original_prompt") String originalPrompt,
    @JsonProperty("code_content") String codeContent,
    List<Map<String, Object>> vulnerabilities,
    @JsonProperty("complexity_details") List<Map<String, Object>> complexityDetails
) {}