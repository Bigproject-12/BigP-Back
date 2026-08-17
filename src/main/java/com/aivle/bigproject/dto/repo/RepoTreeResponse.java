package com.aivle.bigproject.dto.repo;

import java.util.List;
import java.time.LocalDateTime;

/**
 * GitHub 저장소의 트리 구조를 반환하기 위한 응답 DTO.

 */
public record RepoTreeResponse(
        Integer repoId,
        String branch,
        boolean truncated,
        List<Item> items
) {
        /**
         * GitHub 저장소의 트리 구조를 나타내는 내부 클래스 Item.
         * 각 Item은 경로, 타입, SHA, 크기, 분석 상태 및 품질 점수 등의 정보를 포함
         */
    public record Item(
            String path,
            String type,
            String sha,
            Long size,
            int totalIssueCount,
            int securityIssueCount,
            int inefficiencyIssueCount,
            int otherIssueCount,
            boolean analyzed,
            Integer analysisId,
            LocalDateTime lastAnalyzedAt,
            Double qualityScore,
            int criticalIssueCount,
            int highIssueCount,
            int mediumIssueCount,
            int lowIssueCount
    ) {}
}
