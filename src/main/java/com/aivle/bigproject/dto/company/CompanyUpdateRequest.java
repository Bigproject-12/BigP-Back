package com.aivle.bigproject.dto.company;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompanyUpdateRequest(
        @NotBlank(message = "회사명은 필수입니다.")
        @Size(max = 255, message = "회사명은 255자 이하여야 합니다.")
        String name
) {
}
