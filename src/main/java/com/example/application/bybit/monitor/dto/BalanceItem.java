package com.example.application.bybit.monitor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BalanceItem {
    @NotBlank
    private Integer minute;

    // BigDecimal 쓰는 이유 Double 쓰면 지수표현식으로 나옴
    private String won;
    private BigDecimal btc;
    private BigDecimal usd;


}
