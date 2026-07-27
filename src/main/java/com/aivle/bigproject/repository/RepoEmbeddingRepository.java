package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RepoEmbeddingRepository extends JpaRepository<RepoEmbedding, Integer> {
    
    Optional<RepoEmbedding> findByGithubRepoIdAndFaissVectorId(Integer repoId, Integer faissVectorId);
    
    void deleteByGithubRepoId(Integer repoId);
    
    boolean existsByGithubRepo_Id(Integer repoId);
}