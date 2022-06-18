package com.example.application.bybit.trace.dto.response;


import lombok.Data;
@Data
public class BybitMyOrderCancelData {
    public Long user_id;
    public String order_id;
    public String symbol;
    public String side;
    public String order_type;
    public Double price;
    public int qty;
    public String time_in_force;
    public String order_status;
    public int last_exec_time;
    public int last_exec_price;
    public int leaves_qty;
    public int cum_exec_qty;
    public int cum_exec_value;
    public int cum_exec_fee;
    public String reject_reason;
    public String order_link_id;
    public String created_at;
    public String updated_at;
}
