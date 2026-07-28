package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DetectRequest(
    @JsonProperty("code_content")
    String codeContent,
    Integer repoId,
    String language,
    String prompt,
    String filePath,
    @JsonProperty("duplicate_snippets")
    List<DuplicateSnippet> duplicateSnippets
) {}
