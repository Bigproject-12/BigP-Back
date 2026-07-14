package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "FINDING")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Finding {

    @Id
    @Column(name = "result_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    private Integer id;

    @Column(name = "anaysis_id", nullable = false)
    private Integer analysisId;

    @Column(name = "inefficiency_result", nullable = false, columnDefinition = "TEXT")
    private String inefficiencyResult;

    @Lob
    @Column(name = "modified_code", nullable = false, columnDefinition = "TEXT")
    private String modifiedCode;

    @Column(name = "secu_result", nullable = false, columnDefinition = "TEXT")
    private String secuResult;

    @Column(name = "duplicate_result", nullable = false, columnDefinition = "TEXT")
    private String duplicateResult;

    @Column(name = "created_at", nullable = false)
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