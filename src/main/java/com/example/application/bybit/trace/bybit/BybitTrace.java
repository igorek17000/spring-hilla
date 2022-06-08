package com.example.application.bybit.trace.bybit;


import lombok.Data;

import java.util.List;

@Data
public class BybitTrace {

    private String topic;
    private List<BybitTraceData> data;

}
