package com.aivle.bigproject.ai.dto;
import java.util.List;

public record RemoveVectorsRequest(Integer repo_id, 
    List<Integer> vector_ids) {}