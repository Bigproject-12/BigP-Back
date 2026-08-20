package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.UserRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
// 사용자와 GitHub 저장소 간의 연결 정보를 관리하는 Repository
public interface UserRepoRepository extends JpaRepository<UserRepo, Integer> {
    // 사용자와 GitHub 저장소 간의 연결 여부 확인
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);
// 사용자 ID를 기준으로 연결된 GitHub 저장소 조회
    List<UserRepo> findAllByUserId(Integer userId);
// 사용자 ID를 기준으로 연결된 GitHub 저장소 수 조회
    long countByUserId(Integer userId);
// 회사에 연결된 GitHub 저장소 수 조회(중복 제외)
    @Query("""
            SELECT COUNT(DISTINCT ur.githubRepo.id)
            FROM UserRepo ur
            WHERE ur.user.company.id = :companyId
            """)
    long countDistinctRepoByCompanyId(@Param("companyId") Integer companyId);

    Optional<UserRepo> findByUser_IdAndGithubRepo_Id(Integer userId, Integer repoId);
// 사용자의 모든 저장소 연결 정보 삭제 
    @Modifying
    @Query(value = "DELETE FROM USER_REPO WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
//특정 조직에 속한 사용자의 저장소 연결 정보 삭제
    @Modifying
    @Query(value = """
            DELETE FROM USER_REPO 
            WHERE user_id = :userId 
            AND repo_id IN (SELECT repo_id FROM GITHUB_REPO WHERE organization = :orgName)
            """, nativeQuery = true)
    void deleteByUserIdAndOrganization(@Param("userId") Integer userId, @Param("orgName") String orgName);
// 특정 저장소에 연결된 모든 사용자 정보 조회
    List<UserRepo> findAllByGithubRepo_Id(Integer userId);
}
