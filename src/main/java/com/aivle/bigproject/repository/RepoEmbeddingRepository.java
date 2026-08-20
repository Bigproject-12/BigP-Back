package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RepoEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

//저장소의 코드 임베딩 데이터 조회 및 관리 Repository
public interface RepoEmbeddingRepository extends JpaRepository<RepoEmbedding, Integer> {
    // 저장소 ID와 FAISS 벡터 ID를 기준으로 임베딩 조회
    Optional<RepoEmbedding> findByGithubRepoIdAndFaissVectorId(Integer repoId, Integer faissVectorId);
    // 특정 저장소의 모든 임베딩 데이터 삭제
    void deleteByGithubRepoId(Integer repoId);
    // 특정 저장소의 임베딩 데이터 존재 여부 확인
    boolean existsByGithubRepo_Id(Integer repoId);
    // 저장소 ID와 파일 경로를 기준으로 임베딩 목록 조회
    List<RepoEmbedding> findByGithubRepo_IdAndFilePath(Integer repoId, String filePath);
}