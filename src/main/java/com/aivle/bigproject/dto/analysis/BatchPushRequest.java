package com.aivle.bigproject.dto.analysis;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record BatchPushRequest(
        @NotEmpty @Size(max = 20) List<@NotNull @Positive Integer> analysisIds,
        String baseBranch,
        @Size(max = 255) String title,
        String body
) {
}
