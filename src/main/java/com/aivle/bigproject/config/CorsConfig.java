package com.aivle.bigproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 애플리케이션의 CORS(Cross-Origin Resource Sharing) 정책을 설정하는 클래스.
 *
 * 프론트엔드와 백엔드가 서로 다른 Origin에서 실행되는 경우
 * 브라우저의 동일 출처 정책(Same-Origin Policy)으로 인해 발생하는
 * 요청 제한을 허용하기 위해 CORS 설정을 적용한다.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    private final String allowedOrigin;

    public CorsConfig(@Value("${app.cors-allowed-origin}") String allowedOrigin) {
        this.allowedOrigin = allowedOrigin;
    }

    /**
     * 애플리케이션의 전역 CORS 정책을 설정한다.
     *
     * 모든 API 경로에 대해 지정된 Origin의 요청을 허용하며,
     * REST API에서 사용하는 HTTP 메서드와 요청 헤더를 허용한다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                      // 모든 API 경로에 적용
                .allowedOrigins(allowedOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);           // 쿠키/인증정보 허용
    }
}
