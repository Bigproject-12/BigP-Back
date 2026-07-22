package com.aivle.bigproject.ai.dto;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisResultResponse {
    private Integer analysisId;
    private String status;
    private String originCode;
    
    private Integer totalIssues;
    private String secuResult;
    private String inefficiencyResult;
    private String modifiedCode;

    public static AnalysisResultResponse of(Analysis analysis, Finding finding) {
        AnalysisResultResponseBuilder builder = AnalysisResultResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .originCode(analysis.getOriginCode());

        if (finding != null) {
            builder.totalIssues(finding.getTotalIssues())
                   .secuResult(finding.getSecuResult())
                   .inefficiencyResult(finding.getInefficiencyResult())
                   .modifiedCode(finding.getModifiedCode());
        }
        return builder.build();
    }
}