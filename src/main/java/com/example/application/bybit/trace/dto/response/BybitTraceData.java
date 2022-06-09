package com.example.application.bybit.trace.dto.response;

import lombok.Data;
import com.example.application.bybit.trace.enums.*;

@Data
public class BybitTraceData {
    private String timestamp;
    private Long trade_time_ms;
    private SYMBOL symbol;
    private SIDE side;
    private Integer size;

    // TODO: https://bybit-exchange.github.io/docs/inverse/#price-price 확인해봐야함
    private Integer price;
    private TICK_DIRECTION tick_direction;
    private String trade_id;
    private Long cross_seq;
}
