package com.aivle.bigproject.dto.user;

import com.aivle.bigproject.entity.User;
import java.time.LocalDate;

/**
 * 회원가입과 사용자 조회 결과에 사용하는 응답 DTO.
 * 엔티티를 직접 반환하지 않아 password 같은 민감 정보가 노출되는 것을 막는다.
 */

public record UserResponse (
        Integer id,
        String name,
        Integer companyId,
        String loginId,
        String role,
        String gitId,
        String gitName,
        LocalDate createdAt

) {
    /** User 엔티티에서 클라이언트에 공개할 필드만 골라 응답 DTO로 변환한다. */
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                // 회사가 지정되지 않은 사용자도 변환할 수 있도록 null을 처리한다.
                user.getCompany() == null ? null : user.getCompany().getId(),
                user.getLoginId(),
                user.getRole(),
                user.getGitId(),
                user.getGitName(),
                user.getCreatedAt()
        );
    }
}
