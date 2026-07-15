package com.aivle.bigproject.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API가 똑같은 모양으로 응답하게 만드는 클래스.
 *
 * 성공하면: { "success": true, "data": 실제내용 }
 * 실패하면: { "success": false, "message": "왜 실패했는지", "code": "A001" }
 *
 * 겉모양은 항상 같고, data 안의 내용만 API마다 달라진다.
 * → 프론트는 응답 까는 방법(프론트가 서버응답에서 원하는 데이터를 꺼내는 것)을 한 번만 배우면 됨.
 *
 * 주의: new ApiResponse(...) 로는 못 만든다. (생성자를 막아둠-> private)
 *      대신 ApiResponse.ok(...) 또는 ApiResponse.fail(...) 을 쓸 것.
 *      성공인데 에러코드가 붙은 이상한 응답을 못 만들게 하려고.
 */

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 생성자를 private으로 해서 밖에서 new 못 함
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON에서 빼줌

public class ApiResponse<T>{
    private final boolean success; // t or f
    private final T data; // 실제 내용 (T는 나중에 입력)
    private final String message; // 사람이 읽을 메세지
    private final String code; // 프론트가 판단할 에러 코드 (예: 'A001')

    // ===성공시===
    /** 데이터를 보낼 때 → ApiResponse.ok(userDto) 조회,목록,생성 등 거의 모든 성공 api + data*/
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true,data,null,null);
    }

    /** 데이터 + 안내 메시지를 같이 보낼 때 성공은 하였지만 사용자에게 할 말이 있을때 -> 예 분석이 시작되었습니다*/
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<> (true, data, message, null);
    }

    /** 보낼 데이터가 없을 때 (삭제 성공 등) → ApiResponse.ok() / 성공여부만 예: 분석결과 삭제 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }


    // ===== 실패할 때 쓰는 것 =====
    // TODO: ErrorCode enum 만들면 fail(ErrorCode) 버전 추가 예정

    /** 에러를 보낼 때 → ApiResponse.fail("A001", "분석 결과가 없습니다") 실패 + 메세지로 사람에게 왜 실패하였는지 실패 코드*/
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, message, code);
    }
}
