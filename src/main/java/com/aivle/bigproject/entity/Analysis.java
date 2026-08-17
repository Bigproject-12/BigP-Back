package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;

/**
 * 코드 분석 요청 및 결과의 기본 정보를 관리하는 엔티티.
 *
 * 사용자가 GitHub 저장소의 특정 파일에 대해 코드 분석을 요청하면
 * 분석 대상 저장소, 사용자, 회사, 원본 코드, 분석 상태 등의 정보를 저장
 * 또한, 분석 결과와 관련된 추가 정보(분석 비율, 커밋 SHA 등)도 포함
 */
@Entity
@Getter
@Builder
@Setter
@Table(name = "ANALYSIS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Analysis {

    @Id
    @Column(name = "analysis_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repo_id", nullable = false)
    private GithubRepo githubRepo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Lob
    @Column(name = "origin_code", nullable = false, columnDefinition = "TEXT")
    private String originCode;

    @Column(nullable = false)
    private String language;

    @Column(name = "file_path",length = 500) // 히스토리 파일경로 추가
    private String filePath;

    @Column(name = "branch")
    private String branch;

    @Column(name = "source_blob_sha", length = 64)
    private String sourceBlobSha;

    /**
     * 개선 코드가 GitHub에 Push된 Commit SHA.
     *
     * 분석 결과의 중복 Push를 방지하고,
     * 이후 Pull Request 생성 시 Commit 정보를 확인하는데 사용
     */
    @Column(name = "pushed_commit_sha", length = 64)
    private String pushedCommitSha;

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false)
    private String status;

    /**
     * 코드 분석 요청이 생성된 일시.
     *
     * Spring Data JPA Auditing을 통해 엔티티 생성 시
     * 현재 시간이 자동으로 기록되도록 설정  
     */
    @CreatedDate
    @Column (name= "created_at", updatable=false)  //분석요청 시간 추가
    private LocalDateTime createdAt;

    @Column(name = "improvable_ratio",precision = 5,scale = 1)
    private BigDecimal improvableRatio; // 이슈가 걸친 고유 줄 수 / 전체 줄 수 * 100

    /**
     * 개선 코드의 GitHub Push 완료 정보를 기록한다.
     *
     * Push가 성공한 경우 생성된 GitHub Commit SHA와
     * Push 완료 시간을 현재 분석 데이터에 기록
     */
    public void markPushed(String commitSha, LocalDateTime pushedAt) {
        this.pushedCommitSha = commitSha;
        this.pushedAt = pushedAt;
    }
}

