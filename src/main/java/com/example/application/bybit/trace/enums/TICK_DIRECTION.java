package com.example.application.bybit.trace.enums;

/**
 * <p>PlusTick- 물가상승</p>
 * <p>ZeroPlusTick- 거래는 이전 거래와 동일한 가격으로 발생하며 이전 거래보다 높은 가격에 발생했습니다.</p>
 * <p>MinusTick- 가격 하락</p>
 * <p>ZeroMinusTick- 이전 거래와 동일한 가격으로 거래가 이루어지며 이전 거래보다 낮은 가격에 거래됨</p>
 */
public enum TICK_DIRECTION {
    PlusTick, ZeroPlusTick, MinusTick, ZeroMinusTick
}
