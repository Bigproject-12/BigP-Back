package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface FindingRepository extends JpaRepository<Finding, Integer> {
    @Modifying
    @Query(value = """
            DELETE FROM FINDING
            WHERE analysis_id IN (
                SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
            )
            """, nativeQuery = true)
    void deleteByAnalysisOwner(@Param("userId") Integer userId);

    Optional<Finding> findByAnalysisId(Integer analysisId);

    @Query("""
            SELECT COALESCE(SUM(f.totalIssues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.securityCount), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiencyCount), 0) AS inefficiencyIssueCount
            FROM Finding f
            WHERE f.analysis.user.id = :userId
            """)
    IssueCountSummary sumIssueCountsByUserId(@Param("userId") Integer userId);

    interface IssueCountSummary {
        long getTotalIssueCount();
        long getSecurityIssueCount();
        long getInefficiencyIssueCount();
    }
}
