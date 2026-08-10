package com.aivle.bigproject.ai.dto;

/**
 * 중복 코드 탐지 결과의 상세 정보를 전달하는 DTO.
 *
 * 코드 유사도 분석을 통해 탐지된 중복 코드에 대해
 * 파일 위치, 함수 정보, 코드 범위 및 유사도 점수 등의 정보를 저장
 */
public record DuplicateCodeInfo(
    String filePath,
    String functionName,
    String parameters,  
    Integer startLine,
    Integer endLine,
    String code,
    Double similarityScore
) {}