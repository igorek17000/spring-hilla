package com.example.application.bybit.trace.dto.response;


import com.example.application.bybit.trace.enums.SIDE;
import lombok.Data;

@Data
public class BybitPublicOrderData {
    public String symbol;
    public Double price;
    public int size;
    public SIDE side;
}

