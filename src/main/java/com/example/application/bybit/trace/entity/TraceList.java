package com.example.application.bybit.trace.entity;

import com.example.application.bybit.trace.dto.response.BybitOrderData;
import com.example.application.bybit.trace.enums.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TraceList {

    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "trace_idx", referencedColumnName = "idx", nullable = false)
    private Trace trace;

    private Integer level;

    private Long userId;
    private Double price = 0.0;
    private Double stopLossPrice = 0.0;
    private boolean isOk = false;
    private Integer qty = 0;
    private String  orderId = "";
    private String orderLinkId = "";

    @Enumerated(EnumType.STRING)
    private ORDER_TYPE orderType;

    @Enumerated(EnumType.STRING)
    private TIME_IN_FORCE timeInForce;

    @Enumerated(EnumType.STRING)
    private SIDE side;

    @Enumerated(EnumType.STRING)
    private SYMBOL symbol;

    @Enumerated(EnumType.STRING)
    private ORDER_STATUS orderStatus;

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

    public TraceList(BybitOrderData result) {
        this.level = 0;
        this.userId = result.user_id;
        this.price = result.getPrice();
        this.isOk = false;
        this.qty = result.getQty();
        this.orderId = result.getOrder_id();
        this.orderLinkId = result.getOrder_link_id();
        this.orderType = ORDER_TYPE.valueOf(result.getOrder_type());
        this.timeInForce = TIME_IN_FORCE.valueOf(result.getTime_in_force());
        this.side = SIDE.valueOf(result.getSide());
        this.symbol = SYMBOL.valueOf(result.getSymbol());
        this.orderStatus = ORDER_STATUS.valueOf(result.getOrder_status());
        this.createDate = LocalDateTime.now();
    }

    // 남은 결과값들
    // "result": {
    //    "last_exec_time": 0,
    //    "last_exec_price": 0,
    //    "leaves_qty": 1,
    //    "cum_exec_qty": 0,
    //    "cum_exec_value": 0,
    //    "cum_exec_fee": 0,
    //    "reject_reason": "",
    //    "order_link_id": "",
    //    "created_at": "2019-11-30T11:03:43.452Z",
    //    "updated_at": "2019-11-30T11:03:43.455Z"
    //  },
}
