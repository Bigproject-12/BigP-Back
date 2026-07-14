package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(neme = "GITHUB_REPO")
@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GithubRepo {

    @Id
    @Column(name = "repo_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "repo_url", nullable = false)
    private String repoUrl;

    @Coliumn(name = "created_at", nullable = false)
    private LocalDate createdAt;
}