package com.example.application.bybit.trace.dto.response;


import lombok.Data;
import com.example.application.bybit.trace.enums.*;

@Data
public class BybitMyOrderData {

    private Long user_id;
    private ORDER_STATUS order_status;
    private SYMBOL symbol;
    private SIDE side;
    private ORDER_TYPE order_type;
    private String price;
    private String qty;
    private TIME_IN_FORCE time_in_force;
    private String order_link_id;
    private String order_id;


    private String timestamp;
    private Long trade_time_ms;
    private Integer size;

    private TICK_DIRECTION tick_direction;
    private String trade_id;
    private Long cross_seq;

}
