package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.dto.user.PasswordChangeRequest;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.repository.CompanyRepository;
import com.aivle.bigproject.repository.UserRepository;
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
        User user = User.builder()
                .id(1)
                .name("로그인테스트")
                .loginId("test1@example.com")
                .password("encoded-password")
                .role("USER")
                .build();
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        var response = userService.getMe(1);

        assertEquals(1, response.id());
        assertEquals("test1@example.com", response.loginId());
    }
}
