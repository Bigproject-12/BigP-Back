package com.aivle.bigproject.dto.analysis;

import com.aivle.bigproject.entity.Analysis;
import com.aivle.bigproject.entity.Finding;
import java.time.LocalDateTime;

//레포 상세 - 히스토리 목록 조회용

public record AnalysisHistoryResponse (
    Integer id,
    String filePath,
    LocalDateTime analyzedAt,
    Integer issueCount,
    Integer improvementRate, // 개선율은 추가 설계 필요
    String status

){
    public static AnalysisHistoryResponse of (Analysis analysis, Finding finding){
        return new AnalysisHistoryResponse(
                analysis.getId(),
                analysis.getFilePath(),
                analysis.getCreatedAt(),
                finding != null? finding.getTotalIssues() : null, // 진행중 / 실패 / 취소에는 finding이 없으므로
                null, // 개선율
                analysis.getStatus()
        );
    }
}


