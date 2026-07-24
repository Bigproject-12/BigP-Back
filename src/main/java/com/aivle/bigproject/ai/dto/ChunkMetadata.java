package com.aivle.bigproject.ai.dto;

public record ChunkMetadata(
    String file_path,
    Integer start_line,
    Integer end_line,
    Integer faiss_vector_id
) {}