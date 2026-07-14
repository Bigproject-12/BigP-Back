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
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Column(nullable = false)
    @Setter
    private String name;

    @Column(name = "company_id")
    @Setter
    private String companyId;

    @Column(name = "login_id", ullable = false, unique = true)
    private String loginId;

    @Column(ullable = false)
    @Setter
    private String password;

    @Column(ullable = false)
    @Setter
    private String role;

    @Column(name = "git_id")
    @Setter
    private String gitId;

    @Column(name = "git_name")
    @Setter
    private String gitName;

    @Column(name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column(name = "update_at", nullable = false)
    private LocalDate updatedAt;
}