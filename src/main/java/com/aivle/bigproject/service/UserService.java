package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.user.LoginResponse;
import com.aivle.bigproject.dto.user.LoginRequest;
import com.aivle.bigproject.dto.user.PasswordChangeRequest;
import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    public UserService(
            UserRepository userRepository,
            CompanyRepository companyRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
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
        // 기존 회사가 있으면 사용하고, 없으면 신규 회사를 등록한다.
        Company company = findOrCreateCompany(companyName);
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

    /**
     * 로그인 ID와 비밀번호를 검증하고 인증된 사용자를 반환한다.
     * 사용자 존재 여부가 외부에 노출되지 않도록 두 실패 경우 모두 같은 예외를 사용한다.
     * 다음 JWT 구현 단계에서 이 반환값으로 Access Token을 생성한다.
     */
    public User authenticate(LoginRequest request) {
        String loginId = normalizeLoginId(request.loginId());

        User user = userRepository.findByLoginIdIgnoreCase(loginId)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return user;
    }

    /** 자격 증명을 검증하고 API 요청에 사용할 JWT Access Token을 발급한다. */
    public LoginResponse login(LoginRequest request) {
        User user = authenticate(request);
        String accessToken = jwtTokenProvider.createAccessToken(user);

        return LoginResponse.of(
                accessToken,
                user.getId(),
                user.getName(),
                user.getRole()
        );
    }

    /** 현재 비밀번호를 검증한 뒤 새 비밀번호를 암호화하여 저장한다. */
    @Transactional
    public void changePassword(Integer userId, PasswordChangeRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new CustomException(ErrorCode.WRONG_PASSWORD);
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
    }

    /** 현재 로그인한 사용자의 공개 가능한 회원정보를 반환한다. */
    public UserResponse getMe(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
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

    /**
     * 회사명을 대소문자 구분 없이 조회한다.
     */
    private Company findOrCreateCompany(String companyName) {
        return companyRepository.findByNameIgnoreCase(companyName)
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .name(companyName)
                                .build()
                ));
    }

    // 로그인 ID 정규화 및 앞뒤 공백을 자동으로 제거 , 대소문자를 구분하지 않는 방식으로 로그인 정책을 구현 
    private String normalizeLoginId(String loginId) {
        return loginId.trim().toLowerCase(Locale.ROOT);
    }

    @Transactional
    public void saveGithubToken(Integer userId, String githubToken) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        user.updateGithubToken(githubToken);
    }
}
