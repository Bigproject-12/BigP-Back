pakage com.aivle.bigproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@entity
@builder
@Getter
@Table(name = "USER")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class USER{

    @ID
    @GeneratedValue(strategy = GenerationType.PROTECTED)
    @Column(name = "user_id")
    private Integer id;

    @Column(name = "company_id")
    private String companyId;

    @Column(name = "login_id", ullable = false, unique = true)
    private String loginId;

    @Column (name = "password")
    private String password;

    @Column (name = "role")
    private String role;

    @Column (name = "git_id")
    private String gitId;

    @Column (name = "git_name")
    private String gitName;

    @Column (name = "created_at", nullable = false)
    private LocalDate createdAt;

    @Column (name = "update_at", nullable = false)
    private LocalDate updatedAt;
}