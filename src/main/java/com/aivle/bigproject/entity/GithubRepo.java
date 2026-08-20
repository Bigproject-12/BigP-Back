package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
/**
 * GitHub 저장소 정보를 관리하는 엔티티
 * 저장소명, URL, 언어, 마지막 업데이트 시간, 공개 여부 등의 정보를 저장
 */
@Entity
@Table(name = "GITHUB_REPO")
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

    @Column(name = "language")
    private String language; 

    @Column(name = "last_updated")
    private String lastUpdated;

    @Column(name = "is_private")
    private Boolean isPrivate;

    @Column(nullable = false)
    private String organization;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(name = "webhook_id")
    private Long webhookId;

    @Column(name = "webhook_active")
    private Boolean webhookActive;

    /**
     * GitHub 저장소의 Webhook 정보를 업데이트하는 메서드
     * Webhook ID와 활성화 상태를 업데이트
     */
    public void updateWebhookInfo(Long webhookId, Boolean webhookActive) {
        this.webhookId = webhookId;
        this.webhookActive = webhookActive;
    }
}