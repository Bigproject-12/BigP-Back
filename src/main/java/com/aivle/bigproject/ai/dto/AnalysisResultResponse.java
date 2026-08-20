package com.aivle.bigproject.ai.dto;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

/** AI 코드 분석 결과 응답 DTO
 * Analysis 엔티티의 기본 분석 정보와
 * Finding 엔티티의 상세 분석 결과값을 조합하여 클라이언트에 반환.
 */
@Getter
@Builder
public class AnalysisResultResponse {
    // 분석 ID, 상태, 원본 코드
    private Integer analysisId;
    private String status;
    private String originCode;

    // GIthub 레포지토리 정보, 브랜치 경로, 코드 언어, 개선 가능 비율
    private Integer repoId;
    private String repoName;
    private String branch;
    private String language;
    private String filePath;
    private BigDecimal improvableRatio;

    //상세 분석 결과값: 분석 결과, 전체 이슈 개수, 보안 취약점, 비효율성 분석 결과 
    private String duplicateResult;
    private Integer totalIssues;
    private String secuResult;
    private String inefficiencyResult;
    private String modifiedCode;
    private Boolean aiGenerated;
    private Double aiProbability;

    //AI 코드 분석 결과 응답 DTO
    // Analysis 엔티티의 기본 분석 정보와
    // Finding 엔티티의 상세 분석 결과를 조합하여 클라이언트로 반환한다.
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

        /*
         * Finding이 존재하는 경우에만 상세 분석 결과를 설정한다.
         *
         * 분석이 아직 완료되지 않았거나 Finding 데이터가 생성되지 않은 경우
         * NullPointerException이 발생하지 않도록 null 여부를 확인한다.
         */
        if (finding != null) {
            builder.totalIssues(finding.getTotalIssues())
                   .secuResult(finding.getSecuResult())
                   .inefficiencyResult(finding.getInefficiencyResult())
                   .duplicateResult(finding.getDuplicateResult())
                   .modifiedCode(finding.getModifiedCode())
                   .aiGenerated(finding.isAiGenerated())
                   .aiProbability(finding.getAiProbability());
        }
        return builder.build();
    }
}