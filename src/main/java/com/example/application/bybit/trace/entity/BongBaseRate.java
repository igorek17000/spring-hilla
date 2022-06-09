package com.example.application.bybit.trace.entity;

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
public class BongBaseRate {
    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "base_idx", referencedColumnName = "idx")
    private BongBase bongBase;

    private Double rate; // 절대값[(목표가 - 현재값)]에서 증가비율
    private Integer traceRate; // 100기준 판매량 정도
    private Double lossRate;

    // 무조건 시작 1 -> TraceList Level Column 활용
    private Integer sort;

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
