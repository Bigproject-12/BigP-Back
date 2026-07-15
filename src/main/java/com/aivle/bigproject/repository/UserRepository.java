package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByLoginId(String loginId);// id로 DB 검색하는 로그인용 함수
    Optional<User> findByGitId(String gitId); // gitid로 DB 검색하는 로그인용 함수

    boolean existsByLoginId(String loginId); // id가 DB에 존재하는지 검사하는 함수
}