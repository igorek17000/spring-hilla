package com.example.application.bybit.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
public class ExecuteItem {
    @NotBlank
    private Integer minute;
    private String execute_type;
    private String contracts;
    private String qty;
    private String order_price;
    private String exit_type;
    private String trade_time;

}
