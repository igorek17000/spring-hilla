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
public class TraceBaseRate {

    @Id
    @GeneratedValue
    private Integer idx;

    @Column(nullable = false, unique = true)
    private Integer minuteBong = 1;

    // 시작점 금액
    private Double startPrice;
    private Integer startMonth;
    private Integer startDay;
    private Integer startHour;
    private Integer startTime;
    private Integer startMinute;

    // 끝점 금액
    private Double endPrice;
    private Integer endMonth;
    private Integer endDay;
    private Integer endHour;
    private Integer endTime;
    private Integer endMinute;

    // 목표가
    private Double targetPrice;

    // Buy 여부
    private boolean buyFlag = true;


    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
