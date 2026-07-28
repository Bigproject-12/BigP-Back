package com.aivle.bigproject.ai.dto;

public record DuplicateCodeInfo(
    String filePath,
    String functionName,
    String parameters,  
    Integer startLine,
    Integer endLine,
    String code,
    Double similarityScore
) {}