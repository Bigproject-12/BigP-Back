package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 코드 분석 결과를 전달받기 위한 응답 DTO.
 *
 * AI 서버에서 수행한 코드 분석 결과를 기반으로
 * AI 생성 여부, 보안 취약점, 코드 복잡도, 리팩토링 필요 여부,
 * 개선된 코드 및 중복 코드 정보를 AI서버로 부터 전달 받는다.
 */
public record DetectResponse(
    @JsonProperty("patch_success")
    Boolean patchSuccess,

    @JsonProperty("is_ai_generated")
    Boolean isAiGenerated,

    @JsonProperty("ai_probability")
    Double aiProbability,

    @JsonProperty("has_vulnerability")
    Boolean hasVulnerability,

    @JsonProperty("vulnerabilities")
    List<Vulnerability> vulnerabilities,

    @JsonProperty("max_complexity")
    Integer maxComplexity,

    @JsonProperty("needs_refactoring")
    Boolean needsRefactoring,

    @JsonProperty("complexity_details")
    List<ComplexityDetail> complexityDetails,

    @JsonProperty("patched_code")
    String patchedCode,

    @JsonProperty("duplicate_snippets")
    List<DuplicateSnippet> duplicateSnippets
    
) {
    // 취약점 상세 정보용 레코드
    public record Vulnerability(
        @JsonProperty("rule_id")
        String ruleId,
        
        String message,
        
        Integer line
    ) {}

    // 복잡도 상세 정보용 레코드
    public record ComplexityDetail(
        @JsonProperty("function_name")
        String functionName,
        
        @JsonProperty("complexity_score")
        Integer complexityScore,
        
        Integer line,
        
        String message
    ) {}
}
