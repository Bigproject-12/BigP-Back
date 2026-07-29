package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

//Finding 엔티티에 대한 데이터 조회 및 집계 기능을 제공
public interface FindingRepository extends JpaRepository<Finding, Integer> {
        //유저의 모든 분석 결과의 이슈 수를 합산하여 반환
    @Modifying
    @Query(value = """
            DELETE FROM FINDING
            WHERE analysis_id IN (
                SELECT analysis_id FROM ANALYSIS WHERE user_id = :userId
            )
            """, nativeQuery = true)
    void deleteByAnalysisOwner(@Param("userId") Integer userId);

    //유저별 대시보드 정보를 조회
    Optional<Finding> findByAnalysisId(Integer analysisId);

    // 유저별 대시보드 통계 정보를 조회
    @Query("""
            SELECT COALESCE(SUM(f.totalIssues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.securityCount), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiencyCount), 0) AS inefficiencyIssueCount
            FROM Finding f
            WHERE f.analysis.user.id = :userId
            """)
    IssueCountSummary sumIssueCountsByUserId(@Param("userId") Integer userId);


    // 특정 레포지토리의 기간별 이슈 수를 조회
    @Query(value = """
            SELECT COALESCE(SUM(f.total_issues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.security_count), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiency_count), 0) AS inefficiencyIssueCount
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.repo_id = :repoId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    IssueCountSummary sumIssueCountsByRepoIdAndPeriod(
            @Param("repoId") Integer repoId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 특정 레포지토리의 기간별 평균 품질 점수를 조회
    @Query(value = """
            SELECT COALESCE(ROUND(AVG(GREATEST(0,
                       100 - f.security_count * 10
                           - f.inefficiency_count * 5
                           - GREATEST(f.total_issues - f.security_count - f.inefficiency_count, 0) * 3
                   )), 1), 0) AS averageScore
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.repo_id = :repoId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    double averageQualityScoreByRepoIdAndPeriod(
            @Param("repoId") Integer repoId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 회사별 기간별 이슈 수를 조회
    @Query(value = """
            SELECT COALESCE(SUM(f.total_issues), 0) AS totalIssueCount,
                   COALESCE(SUM(f.security_count), 0) AS securityIssueCount,
                   COALESCE(SUM(f.inefficiency_count), 0) AS inefficiencyIssueCount
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.company_id = :companyId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    IssueCountSummary sumIssueCountsByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 회사별 기간별 평균 품질 점수를 조회
    @Query(value = """
            SELECT COALESCE(ROUND(AVG(GREATEST(0,
                       100 - f.security_count * 10
                           - f.inefficiency_count * 5
                           - GREATEST(f.total_issues - f.security_count - f.inefficiency_count, 0) * 3
                   )), 1), 0) AS averageScore
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.company_id = :companyId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            """, nativeQuery = true)
    double averageQualityScoreByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    // 회사별 기간별 일일 품질 점수를 조회
    @Query(value = """
            SELECT DATE(a.created_at) AS analysisDate,
                   ROUND(AVG(GREATEST(0,
                       100 - f.security_count * 10
                           - f.inefficiency_count * 5
                           - GREATEST(f.total_issues - f.security_count - f.inefficiency_count, 0) * 3
                   )), 1) AS averageScore
            FROM FINDING f
            JOIN ANALYSIS a ON a.analysis_id = f.analysis_id
            WHERE a.company_id = :companyId
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
            GROUP BY DATE(a.created_at)
            ORDER BY DATE(a.created_at)
            """, nativeQuery = true)
    List<DailyQualityScore> findDailyQualityScoresByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query(value = """
            SELECT gr.repo_id AS repoId,
                   gr.name AS repoName,
                   ROUND(GREATEST(0,
                       100 - f.securityIssueCount * 10
                           - f.inefficiencyIssueCount * 5
                           - f.otherIssueCount * 3
                   ), 1) AS qualityScore,
                   f.totalIssueCount AS totalIssueCount,
                   f.securityIssueCount AS securityIssueCount,
                   f.inefficiencyIssueCount AS inefficiencyIssueCount,
                   f.otherIssueCount AS otherIssueCount,
                   a.created_at AS lastAnalyzedAt
            FROM ANALYSIS a
            JOIN GITHUB_REPO gr ON gr.repo_id = a.repo_id
            JOIN (
                SELECT analysis_id,
                       SUM(total_issues) AS totalIssueCount,
                       SUM(security_count) AS securityIssueCount,
                       SUM(inefficiency_count) AS inefficiencyIssueCount,
                       SUM(GREATEST(total_issues - security_count - inefficiency_count, 0)) AS otherIssueCount
                FROM FINDING
                GROUP BY analysis_id
            ) f ON f.analysis_id = a.analysis_id
            WHERE a.company_id = :companyId
              AND a.status = 'COMPLETED'
              AND a.created_at >= :from
              AND a.created_at < :toExclusive
              AND EXISTS (
                  SELECT 1 FROM USER_REPO ur
                  JOIN USER u ON u.user_id = ur.user_id
                  WHERE u.company_id = :companyId
                    AND ur.repo_id = a.repo_id
              )
              AND a.analysis_id = (
                  SELECT a2.analysis_id
                  FROM ANALYSIS a2
                  WHERE a2.company_id = :companyId
                    AND a2.repo_id = a.repo_id
                    AND a2.status = 'COMPLETED'
                    AND a2.created_at >= :from
                    AND a2.created_at < :toExclusive
                    AND EXISTS (
                        SELECT 1 FROM FINDING f2
                        WHERE f2.analysis_id = a2.analysis_id
                    )
                  ORDER BY a2.created_at DESC, a2.analysis_id DESC
                  LIMIT 1
              )
            ORDER BY qualityScore ASC,
                     securityIssueCount DESC,
                     totalIssueCount DESC,
                     lastAnalyzedAt DESC
            LIMIT 5
            """, nativeQuery = true)
    List<RiskRepositorySummary> findTop5RiskRepositoriesByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);

    interface IssueCountSummary {
        long getTotalIssueCount();
        long getSecurityIssueCount();
        long getInefficiencyIssueCount();
    }

    // 유저별 기간별 일일 품질 점수를 조회하는 DTO 인터페이스
    interface DailyQualityScore {
        LocalDate getAnalysisDate();
        double getAverageScore();
    }

    interface RiskRepositorySummary {
        Integer getRepoId();
        String getRepoName();
        double getQualityScore();
        long getTotalIssueCount();
        long getSecurityIssueCount();
        long getInefficiencyIssueCount();
        long getOtherIssueCount();
        LocalDateTime getLastAnalyzedAt();
    }
}
