package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@Table(name = "COMPANY")
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Company {

    @Id
    @Column(name = "company_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt;

    public void updateName(String name) {
        this.name = name;
    }
}
