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

import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.ArrayList;

@Service
public class EmbeddingService {

    private final String AI_INDEX_URL = "http://localhost:8000/api/embedding/index"; 
    
    private final RepoEmbeddingRepository repoEmbeddingRepository;
    private final GithubRepoRepository githubRepoRepository;
    private final JsonMapper jsonMapper;

    public EmbeddingService(RepoEmbeddingRepository repoEmbeddingRepository, 
                             GithubRepoRepository githubRepoRepository,
                             JsonMapper jsonMapper) {
        this.repoEmbeddingRepository = repoEmbeddingRepository;
        this.githubRepoRepository = githubRepoRepository;
        this.jsonMapper = jsonMapper;
    }

    @Transactional 
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
                    String parametersJson = jsonMapper.writeValueAsString(
                            chunk.parameters() != null ? chunk.parameters() : List.of()
                    );

                    RepoEmbedding embedding = RepoEmbedding.builder()
                            .githubRepo(githubRepo)
                            .filePath(chunk.file_path())
                            .startLine(chunk.start_line())
                            .endLine(chunk.end_line())
                            .faissVectorId(chunk.faiss_vector_id())
                            .functionName(chunk.function_name())
                            .parameters(parametersJson)
                            .codeSnippet(chunk.code())
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

    private final String AI_SEARCH_URL = "http://localhost:8000/api/embedding/search";

    public List<DuplicateSnippet> searchDuplicates(Integer repoId, String codeContent) {
        SearchDuplicateRequest requestDto = new SearchDuplicateRequest(repoId, codeContent);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<SearchDuplicateRequest> requestEntity = new HttpEntity<>(requestDto, headers);
        RestTemplate restTemplate = new RestTemplate();

        List<DuplicateSnippet> results = new ArrayList<>();

        try {
            SearchDuplicateResponse response = restTemplate.postForObject(AI_SEARCH_URL, requestEntity, SearchDuplicateResponse.class);

            if (response != null && response.duplicates() != null) {
                for (VectorMatch match : response.duplicates()) {
                    repoEmbeddingRepository.findByGithubRepoIdAndFaissVectorId(repoId, match.faiss_vector_id())
                            .ifPresent(embedding -> {
                                List<String> parameters;
                                try {
                                    parameters = jsonMapper.readValue(embedding.getParameters(), List.class);
                                } catch (Exception ex) {
                                    parameters = List.of();
                                }
                                results.add(new DuplicateSnippet(
                                        embedding.getFilePath(),
                                        embedding.getFunctionName(),
                                        parameters,
                                        embedding.getStartLine(),
                                        embedding.getEndLine(),
                                        embedding.getCodeSnippet(),
                                        match.similarity_score()
                                ));
                            });
                }
            }
        } catch (Exception e) {
            System.err.println("중복 코드 검색 중 오류 발생: " + e.getMessage());
        }

        return results;
    }
}