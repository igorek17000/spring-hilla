package com.example.application.bybit.trace.enums;


/**
 * <a href="https://bybit-exchange.github.io/docs/inverse/#order-order">참고 링크</a>
 * Created- 시스템에서 주문을 수락했지만 아직 매칭 엔진을 거치지 않은 경우
 * New- 주문이 성공적으로 완료되었습니다.
 * Rejected - 거부
 * PartiallyFilled - 부분적 구매
 * Filled - 주문이 다 팔린 것
 * PendingCancel- 매칭 엔진이 취소 요청을 받았지만 성공적으로 취소되지 않았을 수 있습니다.
 * Cancelled - 취소
 */
public enum ORDER_STATUS {
    Created, New, Rejected, PartiallyFilled, Filled, PendingCancel, Cancelled
}

