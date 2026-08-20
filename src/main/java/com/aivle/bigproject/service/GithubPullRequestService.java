package com.aivle.bigproject.service;

import com.aivle.bigproject.repository.GithubPullRequestRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

//GitHub Pull Request 상태 동기화를 처리하는 Service
@Service
public class GithubPullRequestService {

    private final GithubPullRequestRepository githubPullRequestRepository;

    public GithubPullRequestService(GithubPullRequestRepository githubPullRequestRepository) {
        this.githubPullRequestRepository = githubPullRequestRepository;
    }
    //GitHub PR의 최신 상태를 GuadRAIl플랫폼에 동기화
    @Transactional
    public boolean synchronizeStatus(
            String organization,
            String repoName,
            Integer githubPrNumber,
            String status,
            LocalDateTime mergedAt
    ) {
        // Organization, 저장소명, PR 번호를 기준으로 관리 중인 PR 조회
        return githubPullRequestRepository
                .findTrackedPullRequest(
                        organization, repoName, githubPrNumber)
                .map(pullRequest -> {
                    // PR 상태 및 병합 시간 업데이트
                    pullRequest.updateStatus(status, mergedAt);
                    return true;
                })
                // 관리 중인 PR이 없으면 false 반환
                .orElse(false);
    }
}
