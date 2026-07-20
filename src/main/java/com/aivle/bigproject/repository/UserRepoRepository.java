package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.entity.UserRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRepoRepository extends JpaRepository<UserRepo, Integer> {
    boolean existsByUserAndGithubRepo(User user, GithubRepo githubRepo);

    List<UserRepo> findAllByUserId(Integer userId);
}