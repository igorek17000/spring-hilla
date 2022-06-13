package com.example.application.bybit.trace.dto.response;


import lombok.Data;

@Data
public class BybitMyOrder {

    private Integer ret_code;
    private String ret_msg;
    private String ext_code;
    private String ext_info;
    private String time_now;
    private Integer rate_limit_status;
    private Long rate_limit_reset_ms;
    private Integer rate_limit;

    private BybitMyOrderResult result;

}

