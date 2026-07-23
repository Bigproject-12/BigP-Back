package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AnalysisRepository extends JpaRepository<Analysis, Integer> {
    @Modifying
    @Query(value = "DELETE FROM ANALYSIS WHERE user_id = :userId", nativeQuery = true)
    void deleteByOwner(@Param("userId") Integer userId);
}
