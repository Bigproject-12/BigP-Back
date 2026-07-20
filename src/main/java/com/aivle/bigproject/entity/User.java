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
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id", nullable = false)
    private Integer id;

    @Column(nullable = false)
    @Setter
    private String name;

    // USER.company_id 외래 키를 Company 엔티티와 연결한다.
    // 지연 로딩을 사용해 실제로 회사 정보가 필요할 때 조회한다.
    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    // 로그인에 사용하는 ID이며 중복 가입을 막기 위해 UNIQUE 제약조건을 둔다.
    @Column(name = "login_id", nullable = false, unique = true)
    private String loginId;

    // 평문이 아닌 PasswordEncoder로 암호화한 비밀번호를 저장 한다.
    @Column(nullable = false)
    @Setter
    private String password;

    // USER, ADMIN 등의 인가 권한을 저장한다.
    @Column(nullable = false)
    @Setter
    private String role;

    @Column(name = "git_id")
    @Setter
    private String gitId;

    @Column(name = "git_name")
    @Setter
    private String gitName;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDate createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDate updatedAt;

    @Setter
    @Column(name = "github_access_token")
    private String githubAccessToken;

    /** 신규 사용자를 저장할 때 생성일과 수정일을 설정한다. */
    @PrePersist
    protected void onCreate() {
        LocalDate now = LocalDate.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** 사용자 정보가 변경될 때 수정일을 갱신한다. */
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDate.now();
    }

    public void updateGithubToken(String githubAccessToken) {
        this.githubAccessToken = githubAccessToken;
    }
}
