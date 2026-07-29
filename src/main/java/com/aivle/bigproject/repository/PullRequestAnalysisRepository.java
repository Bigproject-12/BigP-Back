package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.PullRequestAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestAnalysisRepository
        extends JpaRepository<PullRequestAnalysis, Integer> {

    boolean existsByAnalysis_Id(Integer analysisId);
}
