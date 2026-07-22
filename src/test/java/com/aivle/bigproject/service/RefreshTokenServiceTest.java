package com.aivle.bigproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aivle.bigproject.entity.RefreshToken;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.repository.RefreshTokenRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;

    @Test
    void issueStoresOnlySha256Hash() {
        User user = User.builder().id(1).build();
        when(refreshTokenRepository.findByUserId(1)).thenReturn(Optional.empty());
        when(refreshTokenRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        RefreshTokenService service = new RefreshTokenService(refreshTokenRepository, 1209600);

        String rawToken = service.issue(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertEquals(64, captor.getValue().getTokenHash().length());
        assertNotEquals(rawToken, captor.getValue().getTokenHash());
    }
}
