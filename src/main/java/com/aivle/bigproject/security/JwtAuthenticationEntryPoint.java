package com.aivle.bigproject.security;

import com.aivle.bigproject.dto.common.ErrorResponse;
import com.aivle.bigproject.exception.ErrorCode;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = resolveErrorCode(authException, request);

        response.setStatus(errorCode.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(
                new ErrorResponse(errorCode.name(), errorCode.getMessage())
        ));
    }

    private ErrorCode resolveErrorCode(AuthenticationException authException, HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isBlank()) {
            return ErrorCode.LOGIN_REQUIRED;
        }
        String message = authException.getMessage();
        if (message != null && message.toLowerCase().contains("expired")) {
            return ErrorCode.EXPIRED_TOKEN;
        }
        return ErrorCode.INVALID_TOKEN;
    }
}