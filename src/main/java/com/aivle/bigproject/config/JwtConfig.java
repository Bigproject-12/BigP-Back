package com.aivle.bigproject.config;

import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

//JWT 토큰생성과 검증을 진행 
//HS256(HMAC-SHA256) 알고리즘을 사용하여 토큰을 생성하고, 검증을 통해 토큰의 유효성을 확인
@Configuration
@Slf4j
public class JwtConfig {

    // JWT 토큰생성에 사용할 암호화 키 생성 
    //암호화 키의 경우 application.yml에 등록된 Base64 Secret Key 사용
    @Bean
    public SecretKey jwtSecretKey(@Value("${jwt.secret:}") String encodedSecret) {
        if (encodedSecret == null || encodedSecret.isBlank()) {
            // 개발 환경에서 JWT_SECRET이 설정되지 않은 경우 임시 키를 생성
            log.warn("JWT_SECRET이 없어 개발용 임시 키를 생성합니다. 서버 재시작 시 기존 토큰은 무효화됩니다.");
            return generateDevelopmentKey();
        }

        byte[] keyBytes;
        // Base64로 인코딩된 JWT_SECRET을 디코딩하여 SecretKey 생성
        try {
            keyBytes = Base64.getDecoder().decode(encodedSecret);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_SECRET은 Base64 형식이어야 합니다.", exception);
        }

        // 디코딩된 키의 길이가 32바이트 이상인지 확인
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET은 디코딩 기준 32바이트 이상이어야 합니다.");
        }

        // SecretKeySpec을 사용하여 HmacSHA256 알고리즘에 맞는 SecretKey 생성
        return new SecretKeySpec(keyBytes, "HmacSHA256");
    }

    // JWT 토큰을 생성하는 JwtEncoder 빈 생성
    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    // JWT 토큰을 검증하는 JwtDecoder 빈 생성
    @Bean
    public JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            @Value("${jwt.issuer}") String issuer
    ) {
        // NimbusJwtDecoder를 사용하여 JWT 토큰을 검증
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        // JWT 유효성 검증
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }

    // 개발 환경에서 JWT_SECRET이 설정되지 않은 경우 임시 키를 생성하는 메서드
    // 서버가 재시작되면 새로운 키를 생성 
    private SecretKey generateDevelopmentKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("HmacSHA256");
            keyGenerator.init(256);
            return keyGenerator.generateKey();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("HmacSHA256 키를 생성할 수 없습니다.", exception);
        }
    }
}
