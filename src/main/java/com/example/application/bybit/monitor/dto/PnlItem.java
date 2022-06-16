package com.example.application.bybit.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
@AllArgsConstructor
public class PnlItem {
    @NotBlank
    private Integer minute;

    private String contracts;
    private String closing_direction;
    private String qty;
    private String entry_price;
    private String exit_price;
    private String closed_pnl;
    private String exit_type;
    private String trade_time;

}
