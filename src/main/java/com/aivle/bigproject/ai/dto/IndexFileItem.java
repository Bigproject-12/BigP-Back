package com.aivle.bigproject.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 코드 인덱싱 대상 파일 정보를 전달하는 DTO.
 *
 * AI 서버에서 코드 임베딩 및 벡터 인덱스를 생성할 때 필요한
 * 파일 경로와 실제 코드 내용을 전달한다.
 */

public record IndexFileItem(
    @JsonProperty("file_path") String filePath,
    String content
) {}