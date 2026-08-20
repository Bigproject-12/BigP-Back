package com.aivle.bigproject.security;

import com.aivle.bigproject.dto.common.ApiResponse; // 변경: ErrorResponse → ApiResponse
import com.aivle.bigproject.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

// 인증되지 않은 사용자의 접근 요청을 처리하는 Handler
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();
    //인증 실패 시 원인에 맞는 에러 응답 반환 
    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = resolveErrorCode(authException, request);
        // 인증 실패 원인에 따른 HTTP 상태 코드 및 응답 형식 설정
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(errorCode.name(), errorCode.getMessage()) // 변경: new ErrorResponse → ApiResponse.fail
        ));
    }
    // 인증 요청 상태에 따라 에러 코드 결정
    private ErrorCode resolveErrorCode(AuthenticationException authException, HttpServletRequest request) {
        // Authorization 헤더가 없으면 로그인 필요 처리
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return ErrorCode.LOGIN_REQUIRED;
        }
        // 만료된 토큰인지 확인
        String message = authException.getMessage();
        if (message != null && message.toLowerCase().contains("expired")) {
            return ErrorCode.EXPIRED_TOKEN;
        }
        // 그 외 인증 실패는 유효하지 않은 토큰으로 처리
        return ErrorCode.INVALID_TOKEN;
    }
}