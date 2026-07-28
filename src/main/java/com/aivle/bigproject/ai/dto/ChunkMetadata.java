package com.aivle.bigproject.ai.dto;

import java.util.List;

public record ChunkMetadata(
    String file_path,
    Integer start_line,
    Integer end_line,
    Integer faiss_vector_id,
    String function_name,
    List<String> parameters,
    String code
) {}