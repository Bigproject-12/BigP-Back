package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.UserRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface UserRepoRepository extends JpaRepository<UserRepo, Integer> {
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);

    List<UserRepo> findAllByUserId(Integer userId);

    @Modifying
    @Query(value = "DELETE FROM USER_REPO WHERE user_id = :userId", nativeQuery = true)
    void deleteByUserId(@Param("userId") Integer userId);
}
