package com.aivle.bigproject.dto.user;

import com.aivle.bigproject.entity.User;
import java.time.LocalDate;

/**
 * 조회/가입 결과 등에서 재사용
 * password는 포함 x -> 유저 정보를 밖으로 내보낼 때, 안전한 필드만 고름
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

){
    public static UserResponse from(User user){
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getCompany() == null ? null : user.getCompany().getId(),
                user.getLoginId(),
                user.getRole(),
                user.getGitId(),
                user.getGitName(),
                user.getCreatedAt()
        );
    }

}
