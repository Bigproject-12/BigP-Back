package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DuplicateSnippet(
    @JsonProperty("file_path") String filePath,
    @JsonProperty("function_name") String functionName,
    List<String> parameters,
    @JsonProperty("start_line") Integer startLine,
    @JsonProperty("end_line") Integer endLine,
    String code,
    @JsonProperty("similarity_score") Double similarityScore
) {}