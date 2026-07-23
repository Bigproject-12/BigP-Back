package com.aivle.bigproject.exception;

import com.aivle.bigproject.dto.common.ApiResponse; // 변경: ErrorResponse → ApiResponse
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.security.access.AccessDeniedException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) { // 변경: 반환 타입 ApiResponse<Void>
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.name(), errorCode.getMessage())); // 변경: new ErrorResponse → ApiResponse.fail
    }

    // @valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) { // 변경: 반환 타입 ApiResponse<Void>
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.fail("INVALID_INPUT", message)); // 변경: new ErrorResponse → ApiResponse.fail
    }

    // 예상치 못한 나머지 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) { // 변경: 반환 타입 ApiResponse<Void>
        log.error("서버 오류가 발생했습니다.", e);
        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail( // 변경: new ErrorResponse → ApiResponse.fail
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
                ));
    }

    // DB 제약조건 예외 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) { // 변경: 반환 타입 ApiResponse<Void>
        return ResponseEntity
                .status(ErrorCode.COMPANY_IN_USE.getStatus())
                .body(ApiResponse.fail(ErrorCode.COMPANY_IN_USE.name(), // 변경: new ErrorResponse → ApiResponse.fail
                        ErrorCode.COMPANY_IN_USE.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity
                .status(ErrorCode.NO_PERMISSION.getStatus())
                .body(ApiResponse.fail(ErrorCode.NO_PERMISSION.name(), 
                        ErrorCode.NO_PERMISSION.getMessage()));
    }

}