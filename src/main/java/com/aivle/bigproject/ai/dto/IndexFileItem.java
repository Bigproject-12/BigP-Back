package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record IndexFileItem(
    @JsonProperty("file_path") String filePath,
    String content
) {}