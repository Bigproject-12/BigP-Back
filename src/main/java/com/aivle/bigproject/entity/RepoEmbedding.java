package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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