package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    Optional<RefreshToken> findByUserId(Integer userId);
    void deleteByUserId(Integer userId);
}
