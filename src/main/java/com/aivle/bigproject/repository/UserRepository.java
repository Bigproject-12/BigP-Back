package com.java.bigproject.repository;

import com.java.bigproject.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByLoginId(String loginId);
    Optional<User> findByGitId(String gitId);

    boolean existByLoginId(Strgin loginId);
    void deleteByLoginId(String loginId);
}