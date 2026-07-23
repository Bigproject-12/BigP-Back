package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GithubRepoRepository extends JpaRepository<GithubRepo, Integer> {
    boolean existsByName(String name);

    Optional<GithubRepo> findByName(String name);
}