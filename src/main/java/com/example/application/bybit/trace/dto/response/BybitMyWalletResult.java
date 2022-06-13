package com.example.application.bybit.trace.dto.response;


import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class BybitMyWalletResult {

    @JsonProperty("BTC")
    public BybitMyWalletData bTC;

}

