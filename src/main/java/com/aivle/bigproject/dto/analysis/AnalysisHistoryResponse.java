package com.aivle.bigproject.dto.analysis;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;

import java.math.BigDecimal;
import java.time.LocalDateTime;

//레포 상세 - 히스토리 목록 조회용

public record AnalysisHistoryResponse (
    Integer id,
    String filePath,
    LocalDateTime analyzedAt,
    Integer issueCount,
    BigDecimal improvableRatio, // 개선가능률
    String status

){
    public static AnalysisHistoryResponse of (Analysis analysis, Finding finding){
        return new AnalysisHistoryResponse(
                analysis.getId(),
                analysis.getFilePath(),
                analysis.getCreatedAt(),
                resolveIssueCount(analysis,finding),// 진행중 / 실패 / 취소에는 finding이 없으므로
                analysis.getImprovableRatio(),
                analysis.getStatus()
        );
    }

    // 이슈가 0건일때
    private static Integer resolveIssueCount(Analysis analysis,Finding finding){
        if (finding != null){
            return finding.getTotalIssues();
        }
        return "COMPLETED".equals(analysis.getStatus()) ? 0:null;
    }
}


