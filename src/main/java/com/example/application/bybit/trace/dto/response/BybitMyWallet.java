package com.example.application.bybit.trace.dto.response;


import lombok.Data;

@Data
public class BybitMyWallet {

    public int ret_code;
    public String ret_msg;
    public String ext_code;
    public String ext_info;
    public BybitMyWalletResult result;
    public String time_now;
    public int rate_limit_status;
    public long rate_limit_reset_ms;
    public int rate_limit;

}

