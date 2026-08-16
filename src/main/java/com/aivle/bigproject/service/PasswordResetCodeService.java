package com.aivle.bigproject.service;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * 비밀번호 재설정용 이메일 인증 코드 발급/검증.
 *
 */
@Service
public class PasswordResetCodeService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Duration TTL = Duration.ofMinutes(5);

    private final JavaMailSender mailSender;
    private final Map<String, CodeEntry> codesByLoginId = new ConcurrentHashMap<>();

    public PasswordResetCodeService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void issueAndSend(String loginId) {
        String key = normalize(loginId);
        String code = String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
        codesByLoginId.put(key, new CodeEntry(code, LocalDateTime.now().plus(TTL)));

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(loginId);
        message.setSubject("[GuardrAil] 비밀번호 재설정 인증 코드");
        message.setText("인증 코드: " + code + "\n5분 이내에 입력해주세요.");
        mailSender.send(message);
    }

    /** 코드가 유효한지 확인한다. (비밀번호 재설정용 인증 코드 확인용)
     */
    public boolean verify(String loginId, String code) {
        String key = normalize(loginId);
        CodeEntry entry = codesByLoginId.get(key);
        if (entry == null || entry.expiresAt().isBefore(LocalDateTime.now()) || !entry.code().equals(code)) {
            return false;
        }
        codesByLoginId.put(key, new CodeEntry(entry.code(), LocalDateTime.MAX));
        return true;
    }

    /** 코드가 유효하면 확인 즉시 소모한다 (비밀번호 재설정 최종 제출용). */
    public boolean verifyAndConsume(String loginId, String code) {
        if (!verify(loginId, code)) {
            return false;
        }
        codesByLoginId.remove(normalize(loginId));
        return true;
    }

    private String normalize(String loginId) {
        return loginId.trim().toLowerCase(Locale.ROOT);
    }

    private record CodeEntry(String code, LocalDateTime expiresAt) {}
}
