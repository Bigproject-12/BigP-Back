package com.aivle.bigproject.ai.dto;

import java.util.List;

/**
 * 코드 청크(Chunk)의 메타데이터를 표현하는 DTO.
 *
 * 코드 파일을 일정 단위로 분할하여 임베딩 및 FAISS 벡터 검색에 사용할 때,
 * 각 코드 청크의 위치, 함수 정보, 벡터 ID 등의 메타데이터를 저장한다.
*/
public record ChunkMetadata(
    String file_path,
    Integer start_line,
    Integer end_line,
    Integer faiss_vector_id,
    String function_name,
    List<String> parameters,
    String code
) {}