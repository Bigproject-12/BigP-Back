package com.aivle.bigproject.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** GitHub Access Token을 AES-256-GCM으로 암호화하고 복호화한다. */
@Component
public class GithubTokenCrypto {

    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;
    private final String encodedKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public GithubTokenCrypto(@Value("${github.token-encryption-key}") String encodedKey) {
        this.encodedKey = encodedKey;
    }

    private SecretKeySpec key() {
        byte[] keyBytes;
        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("GITHUB_TOKEN_ENCRYPTION_KEY는 Base64 형식이어야 합니다.", e);
        }
        if (keyBytes.length != 32) {
            throw new IllegalStateException("GITHUB_TOKEN_ENCRYPTION_KEY는 32바이트 키여야 합니다.");
        }
        return new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        byte[] iv = new byte[IV_LENGTH];
        secureRandom.nextBytes(iv);
        byte[] encrypted = crypt(Cipher.ENCRYPT_MODE, plainText.getBytes(StandardCharsets.UTF_8), iv);
        byte[] result = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, result, 0, iv.length);
        System.arraycopy(encrypted, 0, result, iv.length, encrypted.length);
        return Base64.getEncoder().encodeToString(result);
    }

    public String decrypt(String encryptedText) {
        byte[] input = Base64.getDecoder().decode(encryptedText);
        if (input.length <= IV_LENGTH) {
            throw new IllegalArgumentException("암호화된 GitHub 토큰 형식이 올바르지 않습니다.");
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] encrypted = new byte[input.length - IV_LENGTH];
        System.arraycopy(input, 0, iv, 0, iv.length);
        System.arraycopy(input, iv.length, encrypted, 0, encrypted.length);
        return new String(crypt(Cipher.DECRYPT_MODE, encrypted, iv), StandardCharsets.UTF_8);
    }

    private byte[] crypt(int mode, byte[] input, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, key(), new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            return cipher.doFinal(input);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("GitHub 토큰 암호화 처리에 실패했습니다.", e);
        }
    }
}
