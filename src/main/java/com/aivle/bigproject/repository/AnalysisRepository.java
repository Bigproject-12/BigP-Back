package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface AnalysisRepository extends JpaRepository<Analysis, Integer> {
    long countByUserId(Integer userId);

    long countByUserIdAndStatus(Integer userId, String status);

    List<Analysis> findTop5ByUserIdOrderByIdDesc(Integer userId);

    @Modifying
    @Query(value = "DELETE FROM ANALYSIS WHERE user_id = :userId", nativeQuery = true)
    void deleteByOwner(@Param("userId") Integer userId);
}
