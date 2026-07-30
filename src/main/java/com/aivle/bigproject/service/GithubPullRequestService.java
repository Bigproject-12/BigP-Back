package com.aivle.bigproject.service;

import com.aivle.bigproject.repository.GithubPullRequestRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GithubPullRequestService {

    private final GithubPullRequestRepository githubPullRequestRepository;

    public GithubPullRequestService(GithubPullRequestRepository githubPullRequestRepository) {
        this.githubPullRequestRepository = githubPullRequestRepository;
    }

    @Transactional
    public boolean synchronizeStatus(
            String organization,
            String repoName,
            Integer githubPrNumber,
            String status,
            LocalDateTime mergedAt
    ) {
        return githubPullRequestRepository
                .findTrackedPullRequest(
                        organization, repoName, githubPrNumber)
                .map(pullRequest -> {
                    pullRequest.updateStatus(status, mergedAt);
                    return true;
                })
                .orElse(false);
    }
}
