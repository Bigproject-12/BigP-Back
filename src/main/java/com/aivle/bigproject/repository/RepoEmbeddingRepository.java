package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoEmbeddingRepository extends JpaRepository<RepoEmbedding, Integer> {
    
    // 나중에 특정 레포지토리와 벡터 ID로 코드를 역추적할 때 사용할 메서드
    Optional<RepoEmbedding> findByGithubRepoIdAndFaissVectorId(Integer repoId, Integer faissVectorId);
    
    // 특정 레포지토리의 모든 임베딩 기록을 삭제할 때 사용할 메서드
    void deleteByGithubRepoId(Integer repoId);
}