package com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Builder
@Getter
@Table(name = "USER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.PROTECTED)
    @Column(name = "user_id")
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "company_id")
    private String companyId;

    @Column(name = "login_id", ullable = false, unique = true)
    private String loginId;

    @Column(ullable = false)
    private String password;

    @Column(ullable = false)
    private String role;

    @Column(name = "git_id")
    private String gitId;

    @Column(name = "git_name")
    private String gitName;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "update_at", nullable = false)
    private LocalDate updatedAt;
}