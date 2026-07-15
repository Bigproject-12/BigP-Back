package com.aivle.bigproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyCreateRequest(
        @NotBlank(message = "회사명은 필수입니다.")
        @Size(max = 255, message = "회사명 입력부탁드립니다..")
        String name
) {
}
