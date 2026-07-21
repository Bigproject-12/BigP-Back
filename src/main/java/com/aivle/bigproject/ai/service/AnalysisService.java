package com.aivle.bigproject.ai.service;

import com.aivle.bigproject.ai.dto.DetectRequest;
import com.aivle.bigproject.ai.dto.DetectResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AnalysisService {

    // AI 서버 주소
    private final String AI_DETECT_URL = "http://localhost:8000/api/ai/detect";

    public DetectResponse sendToAiServer(DetectRequest requestDto) {
        RestTemplate restTemplate = new RestTemplate();

        // 1. 헤더 설정 (JSON 형태로 보낼 것임을 명시)
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 2. 헤더와 바디(요청 DTO)를 하나로 묶기
        HttpEntity<DetectRequest> requestEntity = new HttpEntity<>(requestDto, headers);

        // 3. FastAPI 서버로 POST 요청 쏘기 (URL, HTTP메서드, 요청데이터, 반환받을타입)
        try {
            return restTemplate.postForObject(AI_DETECT_URL, requestEntity, DetectResponse.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("AI 서버 분석 요청에 실패했습니다.");
        }
    }
}
