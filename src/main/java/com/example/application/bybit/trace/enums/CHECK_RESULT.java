package com.example.application.bybit.trace.enums;

public enum CHECK_RESULT {

    NO_MINUTE_BONG(-5, "몇분봉 세팅인지 알 수 없습니다."),
    ENTER_SET(1, "진입 세팅 중인 값이 존재합니다."),
    EXIT_SET_ERROR(-6, "청산 세팅 중인 값이 이상합니다."),
    SET_COMPLETION(2, "진입, 청산 세팅 후 진행 중인 값이 존재합니다."),
    NO_SLACK(-1, "Slack 설정 데이터가 없습니다."),
    NO_BONG(-7, "봉 데이터가 없습니다.");


    private final Integer code;
    private final String message;

    CHECK_RESULT(Integer code, String message) {
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
