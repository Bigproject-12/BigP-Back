package com.aivle.bigproject.ai.dto;
import java.util.List;

/**
 * 저장소의 벡터 데이터 삭제를 요청하기 위한 DTO.
 */
public record RemoveVectorsRequest(Integer repo_id, 
    List<Integer> vector_ids) {}