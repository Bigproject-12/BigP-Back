package com.aivle.bigproject.ai.dto;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class AnalysisResultResponse {
    private Integer analysisId;
    private String status;
    private String originCode;

    private Integer repoId;
    private String repoName;
    private String branch;
    private String language;
    private String filePath;
    private BigDecimal improvableRatio;

    private Integer totalIssues;
    private String secuResult;
    private String inefficiencyResult;
    private String modifiedCode;
    private Boolean aiGenerated;
    private Double aiProbability;

    public static AnalysisResultResponse of(Analysis analysis, Finding finding) {
        AnalysisResultResponseBuilder builder = AnalysisResultResponse.builder()
                .analysisId(analysis.getId())
                .status(analysis.getStatus())
                .originCode(analysis.getOriginCode())
                .repoId(analysis.getGithubRepo().getId())
                .repoName(analysis.getGithubRepo().getName())
                .language(analysis.getLanguage())
                .filePath(analysis.getFilePath())
                .improvableRatio(analysis.getImprovableRatio())
                .branch(analysis.getBranch());

        if (finding != null) {
            builder.totalIssues(finding.getTotalIssues())
                   .secuResult(finding.getSecuResult())
                   .inefficiencyResult(finding.getInefficiencyResult())
                   .modifiedCode(finding.getModifiedCode())
                   .aiGenerated(finding.isAiGenerated())
                   .aiProbability(finding.getAiProbability());
        }
        return builder.build();
    }
}