package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GithubRepoRepository extends JpaRepository<GithubRepo, Integer> {
}