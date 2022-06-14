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
public class TraceExit {

    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "trace_idx", referencedColumnName = "idx", nullable = false)
    private Trace trace;
    private Integer level; // level -> 1, 2, 3
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
    public TraceExit(BybitOrderData result, Integer level) {
        this.level = level;
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
}
