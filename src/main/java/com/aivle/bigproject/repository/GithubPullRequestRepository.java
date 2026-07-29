package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubPullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubPullRequestRepository
        extends JpaRepository<GithubPullRequest, Integer> {
}
