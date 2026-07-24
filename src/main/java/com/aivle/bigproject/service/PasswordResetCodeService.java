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
 * ponytail: 인메모리(ConcurrentHashMap) 저장이라 서버 재시작 시 코드가 날아가고
 * 인스턴스를 여러 대로 늘리면 안 맞는다. 그게 문제가 되면 RefreshToken처럼
 * DB 테이블(또는 Redis)로 옮길 것. 브루트포스 방지용 시도 횟수 제한도 없음 —
 * 필요해지면 verifyAndConsume에 실패 카운터를 추가.
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

    /**
     * 코드를 소모하지 않고 유효한지만 확인한다 (인증 확인 버튼용).
     * 한 번 통과하면 만료 시각을 없애서, 그 뒤 비밀번호 입력에는 5분 제한이 걸리지 않는다.
     * ponytail: 인증 확인 후엔 사실상 무제한이라 verify만 해놓고 재설정을 안 하면
     * 그 항목이 다음 코드 재발급 전까지 메모리에 남아있음 - 이 앱 규모에선 무시 가능한 수준.
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
