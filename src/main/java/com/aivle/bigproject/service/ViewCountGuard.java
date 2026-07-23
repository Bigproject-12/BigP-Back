package com.aivle.bigproject.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 조회수 중복 방지 + 서버 재시작시 초기화

@Component
public class ViewCountGuard {

    private static final Duration COOLDOWN = Duration.ofSeconds(3);

    // "userId:boardId" → 마지막 조회 시각
    private final Map<String, LocalDateTime> lastViewed = new ConcurrentHashMap<>();

    //조회수를 올려도 되는지 판단 ->  올린다면 시각을 갱신
    public boolean shouldIncrease(Integer userId, Integer boardId) {
        String key = userId + ":" + boardId;
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = lastViewed.get(key);

        // 처음 보거나 쿨다운이 지났으면 허용
        if (last == null || last.plus(COOLDOWN).isBefore(now)) {
            lastViewed.put(key, now);
            return true;
        }
        return false;
    }
}