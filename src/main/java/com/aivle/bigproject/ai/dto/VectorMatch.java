package com.aivle.bigproject.ai.dto;

public record VectorMatch(
    Integer faiss_vector_id,
    Double similarity_score
) {}