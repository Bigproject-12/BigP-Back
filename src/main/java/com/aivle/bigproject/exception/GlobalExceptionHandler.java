package com.aivle.bigproject.exception;

import com.aivle.bigproject.dto.common.ErrorResponse;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.name(),errorCode.getMessage()));
    }
    
    // @valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldError().getDefaultMessage();
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("INVALID_INPUT", message));
    }

    // 예상치 못한 나머지 예외 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("서버 오류가 발생했습니다.", e);
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.name(),
                        ErrorCode.INTERNAL_SERVER_ERROR.getMessage()    
                ));
    }

    // DB 제약조건 예외 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        return ResponseEntity
                .status(ErrorCode.COMPANY_IN_USE.getStatus())
                .body(new ErrorResponse(ErrorCode.COMPANY_IN_USE.name(),
                    ErrorCode.COMPANY_IN_USE.getMessage()));
    }
}
