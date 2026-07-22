package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.user.PasswordChangeRequest;
import com.aivle.bigproject.dto.user.AccountDeleteRequest;
import com.aivle.bigproject.dto.user.UserUpdateRequest;
import com.aivle.bigproject.entity.Company;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.AnalysisRepository;
import com.aivle.bigproject.repository.AnnouncementRepository;
import com.aivle.bigproject.repository.FindingRepository;
import com.aivle.bigproject.repository.NotificationRepository;
import com.aivle.bigproject.repository.RepoFavRepository;
import com.aivle.bigproject.repository.UserRepoRepository;
import com.aivle.bigproject.repository.UserRepository;
import com.aivle.bigproject.security.GithubTokenCrypto;
import com.aivle.bigproject.security.JwtTokenProvider;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock CompanyRepository companyRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock RefreshTokenService refreshTokenService;
    @Mock GithubTokenCrypto githubTokenCrypto;
    @Mock NotificationRepository notificationRepository;
    @Mock FindingRepository findingRepository;
    @Mock AnalysisRepository analysisRepository;
    @Mock AnnouncementRepository announcementRepository;
    @Mock RepoFavRepository repoFavRepository;
    @Mock UserRepoRepository userRepoRepository;
    @InjectMocks UserService userService;

    @Test
    void changePasswordEncodesAndUpdatesPassword() {
        User user = User.builder().id(1).password("encoded-current").build();
        PasswordChangeRequest request =
                new PasswordChangeRequest("Current1!", "Changed1!", "Changed1!");
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current1!", "encoded-current")).thenReturn(true);
        when(passwordEncoder.encode("Changed1!")).thenReturn("encoded-new");

        userService.changePassword(1, request);

        assertEquals("encoded-new", user.getPassword());
        verify(passwordEncoder).encode("Changed1!");
    }

    @Test
    void getMeReturnsUserWithoutPassword() {
        Company company = Company.builder().id(10).name("JWT 테스트 회사").build();
        User user = User.builder()
                .id(1)
                .name("로그인테스트")
                .company(company)
                .loginId("test1@example.com")
                .password("encoded-password")
                .role("USER")
                .build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        var response = userService.getMe(1);

        assertEquals(1, response.id());
        assertEquals("JWT 테스트 회사", response.companyName());
        assertEquals("test1@example.com", response.loginId());
    }

    @Test
    void updateMeChangesNameAndCompany() {
        Company previousCompany = Company.builder().id(10).name("기존 회사").build();
        Company newCompany = Company.builder().id(20).name("새 회사").build();
        User user = User.builder().id(1).name("기존 이름").company(previousCompany).build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(companyRepository.findByNameIgnoreCase("새 회사")).thenReturn(Optional.of(newCompany));

        var response = userService.updateMe(
                1,
                new UserUpdateRequest(" 수정된 이름 ", " 새 회사 ")
        );

        assertEquals("수정된 이름", response.name());
        assertEquals(20, response.companyId());
        assertEquals("새 회사", response.companyName());
    }

    @Test
    void deleteMeRemovesUserAfterPasswordCheck() {
        User user = User.builder().id(1).password("encoded-password").build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Current1!", "encoded-password")).thenReturn(true);

        userService.deleteMe(1, new AccountDeleteRequest("Current1!"));

        verify(notificationRepository).deleteForAccount(1);
        verify(findingRepository).deleteByAnalysisOwner(1);
        verify(analysisRepository).deleteByOwner(1);
        verify(announcementRepository).deleteByUserId(1);
        verify(repoFavRepository).deleteByUserId(1);
        verify(userRepoRepository).deleteByUserId(1);
        verify(userRepository).delete(user);
    }
}
