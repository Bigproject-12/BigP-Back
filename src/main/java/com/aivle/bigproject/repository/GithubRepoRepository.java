package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.GithubRepo;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

// GitHub Repository 정보를 관리하는 Repository
public interface GithubRepoRepository extends JpaRepository<GithubRepo, Integer> {
    // 동일한 저장소명이 존재하는지 확인
    boolean existsByName(String name);
    // 저장소명을 기준으로 저장소 조회
    Optional<GithubRepo> findByName(String name);

    Optional<GithubRepo> findByNameAndOrganization(String name, String organization);
}