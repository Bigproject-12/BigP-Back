package com.aivle.bigproject.ai.dto;

import java.util.List;

public record IndexRepoRequest(
    Integer repo_id,
    List<IndexFileItem> files
) {}