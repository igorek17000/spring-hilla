package com.example.application.bybit.dto.response;


import lombok.Data;

@Data
public class BybitOrder {

    private Integer ret_code;
    private String ret_msg;
    private String ext_code;
    private String ext_info;
    private String time_now;
    private Integer rate_limit_status;
    private Long rate_limit_reset_ms;
    private Integer rate_limit;

    private BybitOrderResult result;

}
