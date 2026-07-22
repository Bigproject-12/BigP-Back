package com.aivle.bigproject.service;

import com.aivle.bigproject.entity.RefreshToken;
import com.aivle.bigproject.entity.User;
import com.aivle.bigproject.exception.CustomException;
import com.aivle.bigproject.exception.ErrorCode;
import com.aivle.bigproject.repository.RefreshTokenRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final long expirationSeconds;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            @Value("${jwt.refresh-token-expiration-seconds}") long expirationSeconds
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.expirationSeconds = expirationSeconds;
    }

    @Transactional
    public String issue(User user) {
        String rawToken = generateToken();
        String tokenHash = hash(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(expirationSeconds);

        RefreshToken refreshToken = refreshTokenRepository.findByUserId(user.getId())
                .orElseGet(() -> RefreshToken.builder().user(user).build());
        refreshToken.rotate(tokenHash, expiresAt);
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Transactional
    public RotatedToken rotate(String rawToken) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new CustomException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        String newRawToken = generateToken();
        refreshToken.rotate(
                hash(newRawToken),
                LocalDateTime.now().plusSeconds(expirationSeconds)
        );
        return new RotatedToken(refreshToken.getUser(), newRawToken);
    }

    @Transactional
    public void revoke(Integer userId) {
        refreshTokenRepository.deleteByUserId(userId);
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    public record RotatedToken(User user, String refreshToken) {}
}
