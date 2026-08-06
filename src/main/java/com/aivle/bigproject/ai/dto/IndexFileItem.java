package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexFileItem(
    @JsonProperty("file_path") String filePath,
    String content
) {}

public record ComplexityDetail(
    @JsonProperty("function_name")
    String functionName,
    
    @JsonProperty("complexity_score")
    Integer complexityScore,
    
    Integer line,
    
    String message
) {}