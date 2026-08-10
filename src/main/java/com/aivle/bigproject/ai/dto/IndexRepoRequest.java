package com.aivle.bigproject.ai.dto;

import java.util.List;

/**
 * GitHub 저장소의 코드 인덱싱을 요청하기 위한 DTO.
 *
 * 저장소 ID와 인덱싱 대상 파일 목록을 AI 서버에 전달하여
 * 코드 임베딩 및 벡터 인덱스 생성을 요청할 때 사용한다.
 */
public record IndexRepoRequest(
    Integer repo_id,
    List<IndexFileItem> files
) {}