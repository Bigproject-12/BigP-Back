package com.aivle.bigproject.ai.dto;

import java.util.List;

public record IndexRepoResponse(
    Integer indexed_chunks,
    List<ChunkMetadata> chunks
) {}