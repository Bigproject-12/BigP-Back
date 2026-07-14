package com.aivle.bigproject.entity;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Builder
@Getter
@Setter
@Table(name = "USER_REPO")
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRepo {

    @Id
    @Column(name = "user_repo_id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id", nullable = false)
    private Integer userId;
    
    @Column(name = "repo_id", nullable = false)
    private Integer repoId;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;
}