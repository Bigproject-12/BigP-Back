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

    @Builder
    public RepoEmbedding(GithubRepo githubRepo, String filePath, Integer startLine, Integer endLine, Integer faissVectorId) {
        this.githubRepo = githubRepo;
        this.filePath = filePath;
        this.startLine = startLine;
        this.endLine = endLine;
        this.faissVectorId = faissVectorId;
    }
}