package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FindingRepository extends JpaRepository<Finding, Integer> {
    @Modifying
    @Query(value = """
            DELETE FROM FINDING
            WHERE analysis_id IN (
                SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
            )
            """, nativeQuery = true)
    void deleteByAnalysisOwner(@Param("userId") Integer userId);
}
