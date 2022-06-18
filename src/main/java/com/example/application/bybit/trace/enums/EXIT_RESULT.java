package com.example.application.bybit.trace.enums;

public enum EXIT_RESULT {

    NO_SLACK(-1, "Slack 설정 데이터가 없습니다."),
    NO_BONG_TARGET_BASE(-2, "봉 진입 기준 데이터가 없습니다."),
    NO_BONG_EXIT_BASE(-3, "봉 청산 기준 데이터가 없습니다."),
    NO_MEMBER(-4, "회원 데이터가 없습니다."),
    ;


    private final Integer code;
    private final String message;

    EXIT_RESULT(Integer code, String message) {
        this.code = code;
        this.message = message;
    }


    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
