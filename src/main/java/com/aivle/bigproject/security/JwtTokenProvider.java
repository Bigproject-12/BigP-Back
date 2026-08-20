package com.aivle.bigproject.security;

import com.aivle.bigproject.entity.User;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Component;

// 로그인 성공 시 JWT Access Token 생성
@Component
public class JwtTokenProvider {

    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final long expirationSeconds;

    public JwtTokenProvider(
            JwtEncoder jwtEncoder,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-expiration-seconds}") long expirationSeconds
    ) {
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    // 사용자 정보를 기반으로 JWT Access Token 생성
    public String createAccessToken(User user) {
        // 토큰 발급 시간 및 만료 시간 설정
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(expirationSeconds, ChronoUnit.SECONDS);
        // JWT 헤더 및 HS256 서명 알고리즘 설정
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();
        // JWT에 포함할 사용자 정보 및 Claim 설정
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("loginId", user.getLoginId())
                .claim("role", user.getRole())
                .build();

        // JWT 인코딩 후 Access Token 반환
                return jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();
    }
}
