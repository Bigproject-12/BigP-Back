package com.aivle.bigproject.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(

        @NotBlank(message = "분석할 코드는 필수입니다.")
        String code,

        @NotBlank(message = "프로그래밍 언어는 필수입니다.")
        String language
) {
}