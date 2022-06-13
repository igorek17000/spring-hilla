package com.example.application.bybit.trace.dto.response;


import lombok.Data;

@Data
public class BybitMyWalletData {
    public int equity;
    public double available_balance;
    public double used_margin;
    public double order_margin;
    public int position_margin;
    public int occ_closing_fee;
    public int occ_funding_fee;
    public int wallet_balance;
    public int realised_pnl;
    public int unrealised_pnl;
    public int cum_realised_pnl;
    public int given_cash;
    public int service_cash;
}

