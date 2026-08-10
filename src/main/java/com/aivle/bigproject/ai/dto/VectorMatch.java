package com.aivle.bigproject.ai.dto;

/**
 * 저장소에 등록된 벡터 데이터 삭제를 요청하기 위한 DTO.
 */
public record VectorMatch(
    Integer faiss_vector_id,
    Double similarity_score
) {}