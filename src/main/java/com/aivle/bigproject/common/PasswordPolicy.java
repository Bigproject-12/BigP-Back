package com.aivle.bigproject.common;

/**
 * 회원가입/비밀번호 변경이 공유하는 비밀번호 규칙.
 * 두 DTO에서 규칙이 어긋나지 않도록 이 클래스만 수정하면 된다.
 */
public final class PasswordPolicy {

    private PasswordPolicy() {
    }

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 18;

    public static final String REGEXP = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s])\\S+$";

    public static final String LENGTH_MESSAGE = "비밀번호는 8자 이상 18자 이하여야 합니다.";
    public static final String PATTERN_MESSAGE = "비밀번호는 영문 대문자, 소문자, 숫자, 특수문자를 각각 하나 이상 포함해야 합니다.";
}
