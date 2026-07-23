package com.aivle.bigproject.ai.service;

import com.aivle.bigproject.ai.dto.*;
import com.aivle.bigproject.entity.GithubRepo;
import com.aivle.bigproject.entity.RepoEmbedding;
import com.aivle.bigproject.repository.GithubRepoRepository;
import com.aivle.bigproject.repository.RepoEmbeddingRepository;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.ArrayList;

@Service
public class EmbeddingService {

    private final String AI_INDEX_URL = "http://localhost:8000/index"; 
    
    // 👇 DB 저장을 위해 의존성 주입 추가
    private final RepoEmbeddingRepository repoEmbeddingRepository;
    private final GithubRepoRepository githubRepoRepository;

    public EmbeddingService(RepoEmbeddingRepository repoEmbeddingRepository, GithubRepoRepository githubRepoRepository) {
        this.repoEmbeddingRepository = repoEmbeddingRepository;
        this.githubRepoRepository = githubRepoRepository;
    }

    @Transactional // 👇 DB 저장 작업이 있으므로 트랜잭션 보장
    public void requestEmbedding(Integer repoId, List<IndexFileItem> fileList) {
        
        // 1. 레포지토리 엔티티 먼저 찾기
        GithubRepo githubRepo = githubRepoRepository.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("레포지토리를 찾을 수 없습니다. ID: " + repoId));

        IndexRepoRequest requestDto = new IndexRepoRequest(repoId, fileList);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<IndexRepoRequest> requestEntity = new HttpEntity<>(requestDto, headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            IndexRepoResponse response = restTemplate.postForObject(AI_INDEX_URL, requestEntity, IndexRepoResponse.class);

            if (response != null && response.chunks() != null) {
                System.out.println("임베딩 완료! DB에 메타데이터 저장을 시작합니다.");
                
                // 매번 save() 하면 느리므로 리스트에 모아서 한 번에 saveAll() 처리
                List<RepoEmbedding> embeddingsToSave = new ArrayList<>();
                
                for (ChunkMetadata chunk : response.chunks()) {
                    RepoEmbedding embedding = RepoEmbedding.builder()
                            .githubRepo(githubRepo)
                            .filePath(chunk.file_path())
                            .startLine(chunk.start_line())
                            .endLine(chunk.end_line())
                            .faissVectorId(chunk.faiss_vector_id())
                            .build();
                            
                    embeddingsToSave.add(embedding);
                }
                
                // 2. 뭉텅이로 DB에 인서트!
                repoEmbeddingRepository.saveAll(embeddingsToSave);
                System.out.println("총 " + embeddingsToSave.size() + "개의 임베딩 데이터 DB 저장 완료!");
            }

        } catch (Exception e) {
            System.err.println("AI 서버 임베딩 요청 또는 DB 저장에 실패했습니다.");
            e.printStackTrace();
        }
    }
}