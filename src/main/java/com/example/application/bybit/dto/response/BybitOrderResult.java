package com.example.application.bybit.dto.response;


import lombok.Data;

import java.util.List;

@Data
public class BybitOrderResult {

    private List<BybitOrderData> data;
    private String cursor;

}