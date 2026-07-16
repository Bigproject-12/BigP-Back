package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    // Spring이 UserService 객체를 생성자 주입한다.
    public UserController(UserService userService) {
        this.userService = userService;
    }

    //회원가입 API: 이메일, 비밀번호 길이,비밀번호 확인 등의 입력값을 검증
    @PostMapping("/signup")
    public ResponseEntity<UserResponse> signup(
            @Valid @RequestBody SignupRequest request
    ) {
        // 중복 확인, 회사 조회, 암호화, DB 저장은 Service에서 수행하도록 설정 
        UserResponse response = userService.signup(request);

        // 회원 생성 성공이므로 HTTP 201로 결과 반환
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}