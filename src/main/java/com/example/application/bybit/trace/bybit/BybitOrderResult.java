package com.example.application.bybit.trace.bybit;


import lombok.Data;

import java.util.List;

@Data
public class BybitOrderResult {

    private List<BybitOrderData> data;
    private String cursor;

}