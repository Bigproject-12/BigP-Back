package com.aivle.bigproject.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 모든 API가 똑같은 모양으로 응답하게 만드는 클래스.
 *
 * 성공(데이터만):      { "success": true,  "data": 실제내용 }
 * 성공(데이터+메시지):  { "success": true,  "data": 실제내용, "message": "분석이 시작되었습니다" }
 * 성공(데이터 없음):    { "success": true }                        // 삭제 성공 등
 * 실패:                { "success": false, "message": "왜 실패했는지", "code": "A001" }
 *
 * (null인 필드는 JSON에서 빠짐 → @JsonInclude(NON_NULL))
 * → 프론트는 success 먼저 보고, 성공이면 data / 실패면 code·message를 꺼내면 됨.
 *
 * 주의: new ApiResponse(...) 로는 못 만든다. (생성자를 private으로 막음)
 *      대신 ok(...) 또는 fail(...) 정적 메서드를 쓸 것.
 *      → 성공인데 에러코드가 붙은 이상한 응답을 못 만들게 하려고.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 생성자를 private으로 해서 밖에서 new 못 함
@JsonInclude(JsonInclude.Include.NON_NULL)        // null인 필드는 JSON에서 빼줌
public class ApiResponse<T> {

    private final boolean success; // 성공 여부 (true / false)
    private final T data;          // 실제 내용 (T는 API마다 달라짐)
    private final String message;  // 사람이 읽을 메시지
    private final String code;     // 프론트가 판단할 에러 코드 (예: "A001")

    // ===== 성공 시 =====

    /** 데이터를 보낼 때 → ApiResponse.ok(userDto). 조회·목록·생성 등 거의 모든 성공 API */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null, null);
    }

    /** 데이터 + 안내 메시지를 같이 보낼 때. 성공했지만 할 말이 있을 때 → 예: "분석이 시작되었습니다" */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message, null);
    }

    /** 보낼 데이터가 없을 때(삭제 성공 등) → ApiResponse.ok(). 성공 여부만 알림 */
    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null, null);
    }

    // ===== 실패 시 =====

    /** 에러를 보낼 때 → ApiResponse.fail("A001", "분석 결과가 없습니다"). 실패 코드 + 사람에게 보여줄 메시지 */
    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, message, code);
    }
}