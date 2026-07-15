package com.aivle.bigproject.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(

        @NotBlank(message = "분석할 코드를 필요로 합니다.")
        String code,

        @NotBlank(message = "해당 코드의 언어를 알려주세요.")
        String language
) {
}