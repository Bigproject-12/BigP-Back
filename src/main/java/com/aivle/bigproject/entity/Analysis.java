package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;

@Entity
@Getter
@Builder
@Setter
@Table(name = "ANALYSIS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Analysis {

    @Id
    @Column(name = "analysis_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "repo_id", nullable = false)
    private Integer repoId;

    @Column(name = "company_id", nullable = false)
    private Integer companyId;

    @Lob
    @Column(name = "origin_code", nullable = false, columnDefinition = "TEXT")
    private String originCode;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private String status;
}