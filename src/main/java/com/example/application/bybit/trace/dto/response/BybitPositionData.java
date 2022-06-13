package com.example.application.bybit.trace.dto.response;

import lombok.Data;

@Data
public class BybitPositionData {

    public int id;
    public int user_id;
    public int risk_id;
    public String symbol;
    public String side;
    public int size;
    public String position_value;
    public String entry_price;
    public boolean is_isolated;
    public int auto_add_margin;
    public String leverage;
    public String effective_leverage;
    public String position_margin;
    public String liq_price;
    public String bust_price;
    public String occ_closing_fee;
    public String occ_funding_fee;
    public String take_profit;
    public String stop_loss;
    public String trailing_stop;
    public String position_status;
    public int deleverage_indicator;
    public String oc_calc_data;
    public String order_margin;
    public String wallet_balance;
    public String realised_pnl;
    public int unrealised_pnl;
    public String cum_realised_pnl;
    public int cross_seq;
    public int position_seq;
    public String created_at;
    public String updated_at;
    public String tp_sl_mode;

}
