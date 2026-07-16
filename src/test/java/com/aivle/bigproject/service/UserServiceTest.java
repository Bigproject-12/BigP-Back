package com.aivle.bigproject.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.user.SignupRequest;
import com.aivle.bigproject.dto.user.UserResponse;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;


// UserService의 회원가입 기능을 테스트를 하기위한 코드 

 //Mockito를 이용하여 Repository와 PasswordEncoder를 Mock 객체로 생성하여
 //실제 DB 없이 비즈니스 로직만 검증을 진행 
 
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CompanyRepository companyRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, companyRepository, passwordEncoder);
    }

    @Test
    void signupNormalizesInputAndEncryptsPassword() {
        // 회원가입 요청 데이터 생성
        SignupRequest request = request(" User@Example.COM ", " github-user ");
        // 회사 정보 생성
        Company company = Company.builder().id(1).name("AIVLE").build();
        // 저장될 사용자 정보 생성
        User savedUser = User.builder()
                .id(10)
                .name("홍길동")
                .company(company)
                .loginId("user@example.com")
                .password("encoded-password")
                .role("USER")
                .gitId("github-user")
                .build();

        when(companyRepository.findByNameIgnoreCase("AIVLE"))
                .thenReturn(Optional.of(company));
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
         // 회원가입 실행
        UserResponse response = userService.signup(request);
        // 반환된 응답 검증
        assertThat(response.id()).isEqualTo(10);
        assertThat(response.loginId()).isEqualTo("user@example.com");
        assertThat(response.role()).isEqualTo("USER");
        // 실제 저장된 User 객체를 가져와 교차 검증 진행 
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        // 비밀번호가 암호화되었는지 검증
        assertThat(captor.getValue().getPassword()).isEqualTo("encoded-password");
    }

    @Test
    void signupRejectsDuplicateLoginId() {
        SignupRequest request = request("user@example.com", "github-user");
        when(userRepository.existsByLoginIdIgnoreCase("user@example.com"))
                .thenReturn(true);

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.ID_ALREADY_EXISTS));

        verify(userRepository, never()).save(any());
    }

    @Test
    void signupRejectsUnknownCompany() {
        SignupRequest request = request("user@example.com", "github-user");
        when(companyRepository.findByNameIgnoreCase("AIVLE"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.signup(request))
                .isInstanceOf(CustomException.class)
                .satisfies(exception -> assertThat(((CustomException) exception).getErrorCode())
                        .isEqualTo(ErrorCode.COMPANY_NOT_FOUND));

        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).save(any());
    }

    private SignupRequest request(String loginId, String gitId) {
        return new SignupRequest(
                "홍길동",
                "AIVLE",
                gitId,
                loginId,
                "password123",
                "password123"
        );
    }
}
