package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * RepoEmbedding 엔티티는 GitHub 저장소의 코드 임베딩 정보를 관리
 * 각 임베딩은 특정 파일의 코드 스니펫, 시작 및 종료 라인, 함수 이름, 매개변수, FAISS 벡터 ID 등의 정보를 포함
 */
@Entity
@Getter
@NoArgsConstructor
@Table(name = "REPO_EMBEDDING")
public class RepoEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id")
    private GithubRepo githubRepo;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "faiss_vector_id", nullable = false)
    private Integer faissVectorId;

    @Column(name = "function_name")
    private String functionName;

    @Lob
    @Column(name = "parameters", columnDefinition = "TEXT")
    private String parameters;   // ["num1", "num2"] 형태의 JSON 문자열로 저장

    @Lob
    @Column(name = "code_snippet", columnDefinition = "TEXT")
    private String codeSnippet;

    /**
     * RepoEmbedding 엔티티의 빌더 메서드
     */
    @Builder
    public RepoEmbedding(GithubRepo githubRepo, String filePath, Integer startLine, Integer endLine, Integer faissVectorId, String functionName, String parameters, String codeSnippet) {
        this.githubRepo = githubRepo;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.faissVectorId = faissVectorId;
        this.functionName = functionName;
        this.parameters = parameters;
        this.codeSnippet = codeSnippet;
    }
}