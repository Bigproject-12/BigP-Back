package com.aivle.bigproject.dto.repo;

import com.aivle.bigproject.entity.GithubPullRequest;
import java.time.LocalDateTime;

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
