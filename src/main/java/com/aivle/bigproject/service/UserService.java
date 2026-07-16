package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.UserRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@Transactional(readOnly = true)
public class UserService {
    //회원가입 시 기본으로 USER 권한 부여
    private static final String DEFAULT_ROLE = "USER";

    //사용자 정보를 조회/저장하는 Repository 
    private final UserRepository userRepository;
    // 회사 정보를 조회하는 Repository 
    private final CompanyRepository companyRepository;
    // 비밀번호 평문 저장을 방지하기위해 암호화 적용  
    private final PasswordEncoder passwordEncoder;

    public UserService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    // 로그인 ID를 소문자로 통일하여 중복 방지
    public UserResponse signup(SignupRequest request) {
        String loginId = normalizeLoginId(request.loginId());
        // GitHub ID와 회사명 앞뒤 공백 제거되어 입력되도록 진행 
        String gitId = request.gitId().trim();
        String companyName = request.companyName().trim();
        // 로그인 ID와 GitHub 계정 중복 여부 체크
        validateDuplicateAccount(loginId, gitId);
        // 회사명이 존재하는지 조회
        Company company = companyRepository.findByNameIgnoreCase(companyName)
                .orElseThrow(() -> new CustomException(ErrorCode.COMPANY_NOT_FOUND));
        // 사용자 엔티티 겍체생성
        User user = User.builder()
                .name(request.name().trim())
                .company(company)
                .loginId(loginId)
                .password(passwordEncoder.encode(request.password()))
                .role(DEFAULT_ROLE)
                .gitId(gitId)
                .build();
        // DB 저장 후 응답 DTO로 변환하여 반환
        return UserResponse.from(userRepository.save(user));
    }
    // 로그인 ID와 GitHub 계정 중복 여부 체크 및 중복된 계정이 존재하면 예외 발생
    private void validateDuplicateAccount(String loginId, String gitId) {
        // 로그인ID 중복 검사 
        if (userRepository.existsByLoginIdIgnoreCase(loginId)) {
            throw new CustomException(ErrorCode.ID_ALREADY_EXISTS);
        }
        // GitHub ID 중복 검사 
        if (userRepository.existsByGitIdIgnoreCase(gitId)) {
            throw new CustomException(ErrorCode.GITHUB_ALREADY_CONNECTED);
        }
    }
    // 로그인 ID 정규화 및 앞뒤 공백을 자동으로 제거 , 대소문자를 구분하지 않는 방식으로 로그인 정책을 구현 
    private String normalizeLoginId(String loginId) {
        return loginId.trim().toLowerCase(Locale.ROOT);
    }
}
