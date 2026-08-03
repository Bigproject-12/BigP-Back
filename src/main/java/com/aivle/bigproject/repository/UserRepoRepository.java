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

public interface UserRepoRepository extends JpaRepository<UserRepo, Integer> {
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);

    List<UserRepo> findAllByUserId(Integer userId);

    long countByUserId(Integer userId);

    @Query("""
            SELECT COUNT(DISTINCT ur.githubRepo.id)
            FROM UserRepo ur
            WHERE ur.user.company.id = :companyId
            """)
    long countDistinctRepoByCompanyId(@Param("companyId") Integer companyId);

    Optional<UserRepo> findByUser_IdAndGithubRepo_Id(Integer userId, Integer repoId);

    @Modifying
    @Query(value = "DELETE FROM USER_REPO WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Query(value = """
            DELETE FROM USER_REPO 
            WHERE user_id = :userId 
            AND repo_id IN (SELECT repo_id FROM GITHUB_REPO WHERE organization = :orgName)
            """, nativeQuery = true)
    void deleteByUserIdAndOrganization(@Param("userId") Integer userId, @Param("orgName") String orgName);

    List<UserRepo> findAllByGithubRepo_Id(Integer userId);
}
