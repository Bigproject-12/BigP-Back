package com.aivle.bigproject.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;

//PBKDF2-HMAC-SHA256 기반 비밀번호 암호화 설정.
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // 비밀번호 암호화 시 PBKDF2-HMAC-SHA256 기반으로 암호화 되도록 설정 
        String encodingId = "pbkdf2@SpringSecurity_v5_8";

        Map<String, PasswordEncoder> encoders = new HashMap<>();
        encoders.put("bcrypt", new BCryptPasswordEncoder());
        encoders.put(
                encodingId,
                Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8()
        );
        // 기본 암호화는 PBKDF2를 사용하고, 저장된 접두사에 따라 적절한 PasswordEncoder를 선택하도록 설정
        DelegatingPasswordEncoder encoder = new DelegatingPasswordEncoder(
                encodingId,
                encoders
        );

        // 기존 BCrypt 설정으로 생성한 접두사 없는 비밀번호도 검증하도록 적용 
        encoder.setDefaultPasswordEncoderForMatches(new BCryptPasswordEncoder());
        return encoder;
    }
}
