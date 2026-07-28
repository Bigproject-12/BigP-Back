package com.aivle.bigproject.ai.dto;

import java.util.List;

public record SearchDuplicateResponse(
    List<VectorMatch> duplicates
) {}