package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 코드 분석 및 탐지 요청 데이터를 전달하는 DTO.
 * 분석 대상 코드와 저장소 정보, 프로그래밍 언어, 프롬프트 등의 정보를 전달하며,
 * 코드 중복 분석 결과가 존재하는 경우 중복 코드 정보도 함께 전달한다.
 */
public record DetectRequest(
    @JsonProperty("code_content")
    String codeContent,
    Integer repoId,
    String language,
    String prompt,
    String filePath,
    String branch,
    @JsonProperty("duplicate_snippets")
    List<DuplicateSnippet> duplicateSnippets
) {}
