package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * 중복 코드 탐지 결과의 코드 조각(Snippet) 정보를 전달하는 DTO.
 *
 * AI 서버의 코드 유사도 분석을 통해 탐지된 중복 코드에 대해
 * 파일 위치, 함수 정보, 파라미터, 코드 범위 및 유사도 점수를 저장한다.
 */
public record DuplicateSnippet(
    @JsonProperty("file_path") String filePath,
    @JsonProperty("function_name") String functionName,
    List<String> parameters,
    @JsonProperty("start_line") Integer startLine,
    @JsonProperty("end_line") Integer endLine,
    String code,
    @JsonProperty("similarity_score") Double similarityScore
) {}