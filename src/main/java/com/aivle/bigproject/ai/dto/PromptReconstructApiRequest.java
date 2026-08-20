package com.aivle.bigproject.ai.dto;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * AI 프롬프트 재구성을 요청하기 위한 DTO.
 *
 * 기존 프롬프트와 분석 대상 코드, 보안 취약점,
 * 코드 복잡도 및 중복 코드 분석 결과를 AI 서버에 전달하여
 * 분석 결과를 반영한 새로운 프롬프트 생성을 요청할 때 사용한다.
 */
public record PromptReconstructApiRequest(
    @JsonProperty("original_prompt") String originalPrompt,
    @JsonProperty("code_content") String codeContent,
    List<Map<String, Object>> vulnerabilities,
    @JsonProperty("complexity_details") List<Map<String, Object>> complexityDetails,
    @JsonProperty("duplicate_snippets") List<Map<String, Object>> duplicateSnippets
) {}