package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * USER 테이블에 접근하는 Spring Data JPA Repository.
 * save, findById, findAll, delete 같은 기본 CRUD 메서드는 JpaRepository가 제공한다.
 */
public interface UserRepository extends JpaRepository<User, Integer> {

    /** 로그인 시 이메일 형식으로 로그인 ID와 사용자를 조회 */
    Optional<User> findByLoginIdIgnoreCase(String loginId);

    /** GitHub 계정 연동 여부를 확인하거나 GitHub ID로 사용자를 조회 */
    Optional<User> findByGitId(String gitId);

    /** 회원가입 전에 로그인 ID 중복 검사 진행  */
    boolean existsByLoginIdIgnoreCase(String loginId);

    /**
     * 특정 회사에 소속된 사용자가 존재하는지 확인한다.
     */
    boolean existsByCompany_Id(Integer companyId);
}
