package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Column(nullable = false)
    private String status;
}