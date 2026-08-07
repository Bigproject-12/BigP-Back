package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DetectResponse(
    @JsonProperty("patch_success")
    Boolean patchStatus,

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
