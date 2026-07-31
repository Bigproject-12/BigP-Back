package com.aivle.bigproject.repository;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

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

    // 로그인 사용자의 레포 단위 히스토리 목록 조회
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
            WHERE a.user.id = :userId
              AND a.githubRepo.id = :repoId
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<AnalysisHistoryResponse> findHistoryByUserIdAndRepoId(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId);

    @Query("""
            SELECT a
            FROM Analysis a
            WHERE a.user.id = :userId
              AND a.githubRepo.id = :repoId
              AND a.branch = :branch
              AND a.status = 'COMPLETED'
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<Analysis> findRecentCompletedForMySpace(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch,
            Pageable pageable);

    @Query("""
            SELECT a
            FROM Analysis a
            WHERE a.user.id = :userId
              AND a.githubRepo.id = :repoId
              AND a.branch = :branch
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<Analysis> findMySpaceHistory(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch,
            Pageable pageable);

    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM Analysis a
            WHERE a.user.id = :userId
              AND a.githubRepo.id = :repoId
              AND a.branch = :branch
              AND a.filePath = :filePath
              AND a.status = 'ANALYZING'
            """)
    boolean existsAnalyzingFile(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch,
            @Param("filePath") String filePath);
}
