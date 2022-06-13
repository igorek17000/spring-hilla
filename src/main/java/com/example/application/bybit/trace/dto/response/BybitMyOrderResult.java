package com.example.application.bybit.trace.dto.response;


import lombok.Data;

import java.util.List;

@Data
public class BybitMyOrderResult {

    private List<BybitMyOrderData> data;
    private String cursor;

}