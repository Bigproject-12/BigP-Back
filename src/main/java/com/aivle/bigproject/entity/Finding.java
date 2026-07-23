package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Table(name = "FINDING")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class Finding {

    @Id
    @Column(name = "result_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private Analysis analysis;

    @Column(name = "inefficiency_result", nullable = false, columnDefinition = "TEXT")
    private String inefficiencyResult;

    @Lob
    @Column(name = "modified_code", nullable = false, columnDefinition = "TEXT")
    private String modifiedCode;

    @Column(name = "secu_result", nullable = false, columnDefinition = "TEXT")
    private String secuResult;

    @Column(name = "duplicate_result", nullable = false, columnDefinition = "TEXT")
    private String duplicateResult;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(name = "is_ai_generated", nullable = false)
    private boolean isAiGenerated;

    @Column(name = "total_issues", nullable = false)
    private Integer totalIssues;

    @Column(name = "security_count", nullable = false)
    private Integer securityCount;

    @Column(name = "inefficiency_count", nullable = false)
    private Integer inefficiencyCount;
}