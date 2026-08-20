package com.aivle.bigproject.repository;

import com.aivle.bigproject.dto.analysis.AnalysisHistoryResponse;
import com.aivle.bigproject.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.domain.Pageable;

public interface AnalysisRepository extends JpaRepository<Analysis, Integer> {

    // 사용자의 전체 분석 개수 조회
    long countByUserId(Integer userId);
    // 사용자의 상태별 분석 개수 조회
    long countByUserIdAndStatus(Integer userId, String status);
    // 사용자, 저장소, 브랜치 기준 분석 개수 조회
    long countByUser_IdAndGithubRepo_IdAndBranch(Integer userId, Integer repoId, String branch);
    //사용자의 최근 분석 5개 조회
    List<Analysis> findTop5ByUserIdOrderByIdDesc(Integer userId);

    //특정 기간 동안의 회사 전체 분석 개수 조회
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
    // 특정 기간 내 사용자의 분석 개수 조회
    @Query(value = """
            SELECT COUNT(*) FROM ANALYSIS
            WHERE user_id = :userId
              AND created_at >= :from
              AND created_at < :toExclusive
            """, nativeQuery = true)
    long countByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
    //특정 기간 내 회사의 상태별 분석 개수 조회
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
        // 특정 기간 내 회사의 최근 분석 7개 조회
    @Query(value = """
            SELECT * FROM ANALYSIS
            WHERE company_id = :companyId
              AND created_at >= :from
              AND created_at < :toExclusive
            ORDER BY analysis_id DESC
            LIMIT 7
            """, nativeQuery = true)
    List<Analysis> findTop7ByCompanyIdAndPeriod(
            @Param("companyId") Integer companyId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
        // 특정 기간 내 사용자의 최근 분석 5개 조회
    @Query(value = """
            SELECT * FROM ANALYSIS
            WHERE user_id = :userId
              AND created_at >= :from
              AND created_at < :toExclusive
            ORDER BY analysis_id DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Analysis> findTop5ByUserIdAndPeriod(
            @Param("userId") Integer userId,
            @Param("from") LocalDateTime from,
            @Param("toExclusive") LocalDateTime toExclusive);
    // 사용자의 모든 분석 내용 삭제 (회원 탈퇴 시)
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
                a.status,
                a.branch,
                CASE WHEN a.pushedCommitSha IS NOT NULL THEN true ELSE false END
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
// 파일별 최근 완료된 분석을 최대 2개씩 조회
    @Query(value = """
            SELECT a.*
            FROM ANALYSIS a
            JOIN (
                SELECT analysis_id,
                       ROW_NUMBER() OVER (
                           PARTITION BY file_path
                           ORDER BY created_at DESC, analysis_id DESC
                       ) AS row_num
                FROM ANALYSIS
                WHERE user_id = :userId
                  AND repo_id = :repoId
                  AND branch = :branch
                  AND status = 'COMPLETED'
                  AND file_path IS NOT NULL
            ) ranked ON ranked.analysis_id = a.analysis_id
            WHERE ranked.row_num <= 2
            ORDER BY a.created_at DESC, a.analysis_id DESC
            """, nativeQuery = true)
    List<Analysis> findLatestTwoPerFile(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch);
// 파일별 가장 최근 완료된 분석의 이슈 요약 조회
    @Query(value = """
            SELECT ranked.file_path AS filePath,
                   ranked.analysis_id AS analysisId,
                   ranked.created_at AS analyzedAt,
                   COALESCE(f.total_issues, 0) AS totalIssueCount,
                   COALESCE(f.security_count, 0) AS securityIssueCount,
                   COALESCE(f.inefficiency_count, 0) AS inefficiencyIssueCount,
                   f.inefficiency_result AS inefficiencyResult
            FROM (
                SELECT analysis_id,
                       file_path,
                       created_at,
                       ROW_NUMBER() OVER (
                           PARTITION BY file_path
                           ORDER BY created_at DESC, analysis_id DESC
                       ) AS row_num
                FROM ANALYSIS
                WHERE user_id = :userId
                  AND repo_id = :repoId
                  AND branch = :branch
                  AND status = 'COMPLETED'
                  AND file_path IS NOT NULL
            ) ranked
            LEFT JOIN FINDING f ON f.analysis_id = ranked.analysis_id
            WHERE ranked.row_num = 1
            """, nativeQuery = true)
    List<FileIssueSummary> findLatestFileIssues(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch);
// 특정 파일의 최근 완료된 분석 조회
    @Query("""
            SELECT a
            FROM Analysis a
            WHERE a.user.id = :userId
              AND a.githubRepo.id = :repoId
              AND a.branch = :branch
              AND a.filePath = :filePath
              AND a.status = 'COMPLETED'
            ORDER BY a.createdAt DESC, a.id DESC
            """)
    List<Analysis> findLatestCompletedFile(
            @Param("userId") Integer userId,
            @Param("repoId") Integer repoId,
            @Param("branch") String branch,
            @Param("filePath") String filePath,
            Pageable pageable);
//분석 ID와 사용자 ID를 기준으로 현재 사용자가 소유한 분석 결과를 조회
    @Query("""
            SELECT a
            FROM Analysis a
            WHERE a.id = :analysisId
              AND a.user.id = :userId
            """)
    Optional<Analysis> findOwnedAnalysis(
            @Param("analysisId") Integer analysisId,
            @Param("userId") Integer userId);
// My Space 저장소 및 브랜치 기준 분석 히스토리 조회
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
// 동일 파일이 현재 분석 중인지 확인
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

 // 파일별 최신 분석 이슈 정보를 반환
    interface FileIssueSummary {
        String getFilePath();
        Integer getAnalysisId();
        LocalDateTime getAnalyzedAt();
        int getTotalIssueCount();
        int getSecurityIssueCount();
        int getInefficiencyIssueCount();
        String getInefficiencyResult();
    }
}
