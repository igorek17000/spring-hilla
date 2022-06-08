package com.example.application.bybit.trace.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TraceMinuteBongBaseRate {
    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "bong_idx", referencedColumnName = "idx")
    private TraceMinuteBongBase traceMinuteBongBase;

    private Double rate;
    private Double lossRate;
    private Integer sort;
}
