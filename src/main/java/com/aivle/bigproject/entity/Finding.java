package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
/**
 * 코드 분석을 통해 탐지된 상세 결과를 관리하는 엔티티.
 * 분석 결과는 비효율성, 보안 취약점, 중복 코드 등의 정보를 포함하며,
 * 각 분석 요청(Analysis)과 연관되어 저장
 * 분석 결과는 원본 코드와 개선된 코드, AI 생성 여부, AI 확률, 총 이슈 수, 보안 이슈 수, 비효율성 이슈 수 등의 정보를 포함
 */
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
/**
 * 상세 분석 결과와 연결된 코드 분석 정보 
 */
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
/**
 * 분석 결과가 AI에 의해 생성되었는지 여부를 나타내는 플래그
 * true이면 AI에 의해 생성된 결과, false이면 사람이 작성한 결과
 */
    @Column(name = "is_ai_generated", nullable = false)
    private boolean isAiGenerated;
// 분석 대상의 코드가 AI가 생성한 확률 값
    @Column(name = "ai_probability")
    private Double aiProbability;

    @Column(name = "total_issues", nullable = false)
    private Integer totalIssues;

    @Column(name = "security_count", nullable = false)
    private Integer securityCount;

    @Column(name = "inefficiency_count", nullable = false)
    private Integer inefficiencyCount;
}