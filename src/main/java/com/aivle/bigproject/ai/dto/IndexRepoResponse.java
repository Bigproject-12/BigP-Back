package com.aivle.bigproject.ai.dto;

import java.util.List;

/**
 * GitHub 저장소 코드 인덱싱 결과를 전달받기 위한 응답 DTO.
 *
 * AI 서버에서 저장소의 코드 파일을 청크 단위로 분할하고
 * 임베딩 및 벡터 인덱싱을 수행한 결과를 전달한다.
 */
public record IndexRepoResponse(
    Integer indexed_chunks,
    List<ChunkMetadata> chunks
) {}