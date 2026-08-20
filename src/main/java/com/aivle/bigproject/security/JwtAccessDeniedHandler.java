package com.aivle.bigproject.security;

import com.aivle.bigproject.dto.common.ApiResponse; // 변경: ErrorResponse → ApiResponse
import com.aivle.bigproject.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

// 인증된 사용자가 권한이 없는 리소스에 접근할 때 발생하는 예외를 처리하는 Handler
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();
    // 접근권한이 없는 요청일 경우  해당 요청에 대해 에러 응답으로 처리
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        // 권한 없음 에러 코드 설정 
        ErrorCode errorCode = ErrorCode.NO_PERMISSION;
        // HTTP 상태 코드 및 응답 형식 설정
        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                ApiResponse.fail(errorCode.name(), errorCode.getMessage()) // 변경: new ErrorResponse → ApiResponse.fail
        ));
    }
}
