package com.aivle.bigproject.controller;

import com.aivle.bigproject.dto.user.AccountDeleteRequest;
import com.aivle.bigproject.dto.user.LoginRequest;
import com.aivle.bigproject.dto.user.LoginResponse;
import com.aivle.bigproject.dto.user.PasswordChangeRequest;
import com.aivle.bigproject.dto.user.PasswordResetCodeRequest;
import com.aivle.bigproject.dto.user.PasswordResetRequest;
import com.aivle.bigproject.dto.user.RefreshTokenRequest;
import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.dto.user.UserUpdateRequest;
import com.aivle.bigproject.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    /** Refresh Token을 검증하고 두 토큰을 모두 새 값으로 교체한다. */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        return ResponseEntity.ok(userService.refresh(request));
    }

    /**
     * 성공 응답을 받은 클라이언트가 보관 중인 Access Token을 삭제한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal Jwt jwt) {
        userService.logout(Integer.valueOf(jwt.getSubject()));
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

    /** 비회원 비밀번호 찾기: 가입된 이메일로 인증 코드를 보낸다. */
    @PostMapping("/password/reset-code")
    public ResponseEntity<Void> requestPasswordResetCode(
            @Valid @RequestBody PasswordResetCodeRequest request
    ) {
        userService.requestPasswordResetCode(request);
        return ResponseEntity.noContent().build();
    }

    /** 이메일 인증 코드를 확인하고 새 비밀번호로 재설정한다. */
    @PostMapping("/password/reset")
    public ResponseEntity<Void> resetPassword(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        userService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }

    /** Access Token에 해당하는 현재 로그인 사용자의 정보를 조회한다. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(
                userService.getMe(Integer.valueOf(jwt.getSubject()))
        );
    }

    /** 현재 로그인한 사용자의 이름과 회사를 부분 수정한다. */
    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        return ResponseEntity.ok(
                userService.updateMe(Integer.valueOf(jwt.getSubject()), request)
        );
    }

    /** 현재 비밀번호를 확인한 뒤 로그인한 사용자의 계정을 삭제한다. */
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteMe(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AccountDeleteRequest request
    ) {
        userService.deleteMe(Integer.valueOf(jwt.getSubject()), request);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> findAll(
            @PageableDefault(size = 20, sort = "id") Pageable pageable
    ) {
        return userService.findAll(pageable);
    }

    /** 관리자가 회원 목록에서 특정 회원을 선택했을 때 상세 정보를 조회한다. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse findById(@PathVariable Integer id) {
        return userService.getUser(id);
    }

    /** 관리자가 회원 상세 화면에서 특정 회원을 강제로 삭제한다. */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Integer id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
