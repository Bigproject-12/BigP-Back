package com.aivle.bigproject.service;

import com.aivle.bigproject.dto.request.AnalysisRequest;
import com.aivle.bigproject.dto.response.AnalysisResponse;
import org.springframework.stereotype.Service;

@Service
public class AnalysisService {

    public AnalysisResponse analyze(AnalysisRequest request) {

        int codeLength = request.code().length();

        String result = String.format(
                "%s 코드 분석이 완료되었습니다. 코드 길이는 %d자입니다.",
                request.language(),
                codeLength
        );

        return new AnalysisResponse(
                request.language(),
                result
        );
    }
}