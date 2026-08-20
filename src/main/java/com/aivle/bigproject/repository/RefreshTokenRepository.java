package com.aivle.bigproject.repository;

import com.aivle.bigproject.entity.RefreshToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
// RefreshToken 정보를 관리하는 Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    //// 토큰 해시값을 기준으로 Refresh Token 조회
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    //// 사용자 ID를 기준으로 Refresh Token 조회
    Optional<RefreshToken> findByUserId(Integer userId);
    //// 사용자 ID를 기준으로 Refresh Token 삭제
    void deleteByUserId(Integer userId);
}
