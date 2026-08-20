package com.aivle.bigproject.dto.repo;

import com.aivle.bigproject.entity.GithubPullRequest;
import java.time.LocalDateTime;

/**
 * GitHub Pull Request 요약 정보를 반환하기 위한 응답 DTO.
 * PullRequestSummaryResponse는 PR 번호, 제목, 작성자, 상태, 생성일 등의 정보를 포함하며,
 * GithubPullRequest 엔티티를 기반으로 생성
 */
public record PullRequestSummaryResponse (
        Integer id,
        Integer prNumber,
        String title,
        String prUrl,
        String repoName,
        String status,
        String authorName,
        LocalDateTime createdAt    
) { 
    /**
     * GithubPullRequest 엔티티를 PullRequestSummaryResponse DTO로 변환
     */
    public static PullRequestSummaryResponse from(GithubPullRequest pr) {
        return new PullRequestSummaryResponse(
            pr.getId(),
            pr.getGithubPrNumber(),
            pr.getTitle(),
            pr.getPrUrl(),
            pr.getGithubRepo().getName(),
            pr.getStatus(),
            pr.getUser().getName(),
            pr.getCreatedAt()
        );
    }
}
