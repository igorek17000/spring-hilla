package com.example.application.bybit.trace.entity;

import lombok.*;

import com.example.application.member.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 봉 거래 시점
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Trace {

    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "member_idx", referencedColumnName = "idx", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer minuteBong    = 1;
    private Double  basePrice     = 0.0; /* 고점, 저점 */
    private Double  price         = 0.0; /* 진입점 금액 */
    private Double  lossPrice     = 0.0; /* 손절 금액 Start 설정해야함 */

    private Double enterEndRate   = 0.0; // 진입하면 안되는 지점
    private Double lossRate       = 0.0; // 손절 위치 지점

    private boolean buyFlag    = true;
    private boolean startFlag  = false;
    private boolean endFlag    = false;

    @OneToMany(mappedBy = "trace", cascade = CascadeType.REMOVE)
    public List<TraceEnter> traceEnters = new ArrayList<>();

    @OneToMany(mappedBy = "trace", cascade = CascadeType.REMOVE)
    public List<TraceExit> traceExits = new ArrayList<>();

    @OneToMany(mappedBy = "trace", cascade = CascadeType.REMOVE)
    public List<TraceExitRate> traceExitsRates = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

}
