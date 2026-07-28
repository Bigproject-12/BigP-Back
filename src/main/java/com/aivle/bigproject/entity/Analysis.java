package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;

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

    @Column(name = "branch", length =255)
    private String branch;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false)
    private String status;

    @CreatedDate
    @Column (name= "created_at", updatable=false)  //분석요청 시간 추가
    private LocalDateTime createdAt;

    @Column(name = "improvable_ratio",precision = 5,scale = 1)
    private BigDecimal improvableRatio; // 이슈가 걸친 고유 줄 수 / 전체 줄 수 * 100
}