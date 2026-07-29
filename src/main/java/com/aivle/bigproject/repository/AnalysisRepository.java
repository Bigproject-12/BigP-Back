package com.aivle.bigproject.repository;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;

public interface AnalysisRepository extends JpaRepository<Analysis, Integer> {

    long countByUserId(Integer userId);

    long countByUserIdAndStatus(Integer userId, String status);

    List<Analysis> findTop5ByUserIdOrderByIdDesc(Integer userId);

    @Query(value = """
            SELECT COUNT(*) FROM ANALYSIS
            WHERE company_id = :companyId
              AND created_at >= :from
              AND created_at < :toExclusive
            """, nativeQuery = true)
    long countByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query(value = """
            SELECT COUNT(*) FROM ANALYSIS
            WHERE company_id = :companyId
              AND status = :status
              AND created_at >= :from
              AND created_at < :toExclusive
            """, nativeQuery = true)
    long countByCompanyIdAndStatusAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("status") String status,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query(value = """
            SELECT * FROM ANALYSIS
            WHERE company_id = :companyId
              AND created_at >= :from
              AND created_at < :toExclusive
            ORDER BY analysis_id DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Analysis> findTop5ByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Modifying
    @Query(value = "DELETE FROM ANALYSIS WHERE user_id = :userId", nativeQuery = true)
    void deleteByOwner(@Param("userId") Integer userId);

    // 레포 단위 히스토리 목록 조회
    @Query("""
            SELECT new com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse(
                a.id,
                a.filePath,
                a.createdAt,
                CASE
                    WHEN f.totalIssues IS NOT NULL THEN f.totalIssues
                    WHEN a.status = 'COMPLETED' THEN 0
                    ELSE NULL
                END,
                a.improvableRatio,
                a.status
            )
            FROM Analysis a
            LEFT JOIN Finding f ON f.analysis = a
            WHERE a.githubRepo.id = :repoId
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<AnalysisHistoryResponse> findHistoryByRepoId(@Param("repoId") Integer repoId);
}
