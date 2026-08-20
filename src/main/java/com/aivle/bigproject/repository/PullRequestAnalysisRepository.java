package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.PullRequestAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

// PullRequest와 분석 결과 정보를 관리하는 Repository
public interface PullRequestAnalysisRepository
        extends JpaRepository<PullRequestAnalysis, Integer> {

    boolean existsByAnalysis_Id(Integer analysisId);
}
