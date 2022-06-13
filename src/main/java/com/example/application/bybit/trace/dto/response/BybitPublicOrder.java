package com.example.application.bybit.trace.dto.response;


import lombok.Data;

import java.util.List;

@Data
public class BybitPublicOrder {
    public int ret_code;
    public String ret_msg;
    public String ext_code;
    public String ext_info;
    public List<BybitPublicOrderData> result;
    public String time_now;
}

