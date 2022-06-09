package com.example.application.bybit.trace.entity;

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

    // TODO: https://bybit-exchange.github.io/docs/inverse/#quantity-qty 확인해봐야함
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
