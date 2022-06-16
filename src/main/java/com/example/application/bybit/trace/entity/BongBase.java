package com.example.application.bybit.trace.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BongBase {

    @Id
    @GeneratedValue
    private Integer idx;

    @OneToMany(mappedBy = "bongBase")
    private List<BongBaseExitRate> exitRates = new ArrayList<>();

    @Column(unique = true, nullable = false)
    private Integer minuteBong;

    private Double enterEndRate; // 진입하면 안되는 지점
    private Double lossRate;     // 손절 위치 지점

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

}
