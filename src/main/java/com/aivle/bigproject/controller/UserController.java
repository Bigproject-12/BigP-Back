package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.LoginRequest;
import com.aivle.bigproject.dto.user.LoginResponse;
import com.aivle.bigproject.dto.user.PasswordChangeRequest;
import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;

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

    /** 로그인 성공 시 이후 API 요청에 사용할 Bearer Access Token을 반환한다. */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        return ResponseEntity.ok(userService.login(request));
    }

    /**
     * 성공 응답을 받은 클라이언트가 보관 중인 Access Token을 삭제한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }

    /** 로그인한 사용자의 현재 비밀번호를 확인하고 새 비밀번호로 변경한다. */
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody PasswordChangeRequest request
    ) {
        userService.changePassword(Integer.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }

    /** Access Token에 해당하는 현재 로그인 사용자의 정보를 조회한다. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                userService.getMe(Integer.valueOf(jwt.getSubject()))
        );
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return userService.findAll(pageable);
    }   
}
