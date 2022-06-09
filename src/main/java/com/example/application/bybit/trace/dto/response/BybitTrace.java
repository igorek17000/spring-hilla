package com.example.application.bybit.trace.dto.response;


import lombok.Data;

import java.util.List;

@Data
public class BybitTrace {

    private String topic;
    private List<BybitTraceData> data;

}
