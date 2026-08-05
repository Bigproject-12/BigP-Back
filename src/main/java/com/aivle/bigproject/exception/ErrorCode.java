package com.aivle.bigproject.exception;

import com.aivle.bigproject.common.PasswordPolicy;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
   
    // 에러코드 목록
    // 로그인 및 회원가입
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    ID_ALREADY_EXISTS(HttpStatus.CONFLICT, "아이디 또는 이메일이 이미 존재합니다."),
    WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "현재 비밀번호가 올바르지 않습니다."),
    NEW_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "새 비밀번호가 새 비밀번호 확인과 일치하지 않습니다."),
    INVALID_PASSWORD_LENGTH(HttpStatus.BAD_REQUEST, PasswordPolicy.LENGTH_MESSAGE),
    INVALID_PASSWORD_PATTERN(HttpStatus.BAD_REQUEST, PasswordPolicy.PATTERN_MESSAGE),
    SAME_AS_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "새 비밀번호가 현재 비밀번호와 동일합니다. 다른 비밀번호를 입력해주세요."),
    INVALID_RESET_CODE(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."),
    SOCIAL_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "소셜 로그인에 실패했습니다."),
    SOCIAL_ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "연동된 소셜 계정을 찾을 수 없습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "로그인 후 이용가능합니다."),

    // 회사
    COMPANY_NOT_FOUND(HttpStatus.NOT_FOUND, "회사를 찾을 수 없습니다."),
    COMPANY_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 기업입니다."),
    COMPANY_IN_USE(HttpStatus.CONFLICT, "소속된 사용자가 있어 삭제할 수 없습니다."),
    COMPANY_NOT_LINKED(HttpStatus.BAD_REQUEST, "사용자에게 회사 정보가 없습니다."),

    // GitHub & 레포지토리
    GITHUB_ALREADY_CONNECTED(HttpStatus.CONFLICT, "이미 연동된 GitHub 계정입니다."),
    GITHUB_TOKEN_NOT_CONNECTED(HttpStatus.BAD_REQUEST, "연동된 GitHub 토큰이 없습니다."),
    GITHUB_API_ERROR(HttpStatus.BAD_GATEWAY, "GitHub API 요청에 실패했습니다."),
    REPO_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 연동된 레포지토리입니다."),
    REPO_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 레포지토리입니다."),
    GITHUB_FILE_FETCH_FAILED(HttpStatus.BAD_REQUEST, "파일 정보를 가져오는데 실패했습니다. 브랜치나 경로를 확인해주세요."),
    GITHUB_BRANCH_FETCH_FAILED(HttpStatus.BAD_REQUEST, "GitHub 브랜치의 최신 정보를 가져오지 못했습니다."),
    GITHUB_COMMIT_FETCH_FAILED(HttpStatus.BAD_REQUEST, "GitHub Commit 정보를 가져오지 못했습니다."),
    GITHUB_FILE_MODE_FETCH_FAILED(HttpStatus.BAD_REQUEST, "GitHub 파일 권한 정보를 가져오지 못했습니다."),
    GITHUB_BLOB_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 파일 Blob 생성에 실패했습니다."),
    GITHUB_TREE_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub 파일 Tree 생성에 실패했습니다."),
    GITHUB_COMMIT_CREATE_FAILED(HttpStatus.BAD_GATEWAY, "GitHub Commit 생성에 실패했습니다."),
    GITHUB_BRANCH_UPDATE_FAILED(HttpStatus.CONFLICT, "작업 중 GitHub 브랜치가 변경되어 push하지 못했습니다."),
    GITHUB_COMMIT_FAILED(HttpStatus.CONFLICT, "Github에 반영하는데 실패했습니다. 그 사이 브랜치가 변경됐을 수도 있습니다. 다시 분석해주세요"),
    GITHUB_DEFAULT_BRANCH_FETCH_FAILED(HttpStatus.BAD_REQUEST, "레포지토리의 기본 브랜치 정보를 가져오는데 실패했습니다."),
    GITHUB_PR_CREATE_FAILED(HttpStatus.CONFLICT, "Pull Request 생성에 실패했습니다."),
    GITHUB_PR_ALREADY_CREATED(HttpStatus.CONFLICT, "해당 분석 결과로 이미 Pull Request가 생성되었습니다."),
    GITHUB_PR_ALREADY_OPEN(HttpStatus.CONFLICT, "이미 같은 base와 compare 브랜치 대상으로 열려있는 PR이 있습니다."),
    GITHUB_PR_SAME_BRANCH(HttpStatus.BAD_REQUEST, "분석 브랜치가 기본 브랜치와 동일하여 PR을 생성할 수 없습니다."),
    GITHUB_PR_HEAD_MISMATCH(HttpStatus.CONFLICT, "push 이후 브랜치가 변경되어 PR을 생성할 수 없습니다."),
    GITHUB_PR_CLOSE_FAILED(HttpStatus.BAD_GATEWAY, "생성된 Pull Request를 정리하지 못했습니다."),
    INVALID_PR_QUERY(HttpStatus.BAD_REQUEST, "PR 조회 조건이 올바르지 않습니다."),
    INVALID_BRANCH(HttpStatus.BAD_REQUEST, "브랜치 정보가 올바르지 않습니다."),
    INVALID_PAGE_REQUEST(HttpStatus.BAD_REQUEST, "페이지 조회 조건이 올바르지 않습니다."),

    // 분석
    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 분석 내용을 찾을 수 없습니다."),
    ANALYSIS_ALREADY_FINISHED(HttpStatus.CONFLICT, "이미 완료된 분석입니다."),
    ANALYSIS_ALREADY_RUNNING(HttpStatus.CONFLICT, "동일한 파일의 분석이 이미 진행 중입니다."),
    SOURCE_NOT_CHANGED(HttpStatus.CONFLICT, "이전 분석 이후 코드가 변경되지 않았습니다."),
    SOURCE_CHANGED_SINCE_ANALYSIS(HttpStatus.CONFLICT, "분석 이후 파일이 변경되었습니다. 최신 코드로 다시 분석해주세요."),
    ANALYSIS_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "완료된 분석만 처리할 수 있습니다."),
    ANALYSIS_ALREADY_PUSHED(HttpStatus.CONFLICT, "이미 push된 분석 결과입니다."),
    ANALYSIS_NOT_PUSHED(HttpStatus.BAD_REQUEST, "push가 완료된 분석 결과만 PR을 생성할 수 있습니다."),
    ANALYSIS_BRANCH_FILE_INFO_MISSING(HttpStatus.BAD_REQUEST, "브랜치 또는 파일 경로 정보가 없어 push할 수 없습니다."),
    ANALYSIS_BRANCH_INFO_MISSING(HttpStatus.BAD_REQUEST, "브랜치 정보가 없어 PR을 생성할 수 없습니다."),
    BATCH_ANALYSIS_DUPLICATED(HttpStatus.BAD_REQUEST, "동일한 분석 결과가 중복 선택되었습니다."),
    BATCH_REPOSITORY_MISMATCH(HttpStatus.BAD_REQUEST, "같은 저장소의 분석 결과만 함께 처리할 수 있습니다."),
    BATCH_BRANCH_MISMATCH(HttpStatus.BAD_REQUEST, "같은 브랜치의 분석 결과만 함께 처리할 수 있습니다."),
    BATCH_COMMIT_MISMATCH(HttpStatus.BAD_REQUEST, "같은 Commit으로 push된 분석 결과만 함께 처리할 수 있습니다."),
    BATCH_FILE_DUPLICATED(HttpStatus.BAD_REQUEST, "동일한 파일의 분석 결과가 중복 선택되었습니다."),
    IMPROVED_CODE_MISSING(HttpStatus.BAD_REQUEST, "개선 코드가 없는 분석 결과는 push할 수 없습니다."),
    FINDING_NOT_FOUND(HttpStatus.NOT_FOUND, "분석 결과가 없습니다."),

    // 권한
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    CANNOT_DELETE_ADMIN(HttpStatus.FORBIDDEN, "운영자 계정은 삭제할 수 없습니다."),

    // 토큰
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh Token이 만료되었습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 Refresh Token입니다."),

    // 대시보드
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "시작일은 종료일보다 늦을 수 없습니다."),

    // 서버
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),

    // 공지사항
    ANNOUNCEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),

    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }
}
