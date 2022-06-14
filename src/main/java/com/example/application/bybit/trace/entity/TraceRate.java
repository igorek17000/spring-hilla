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
public class TraceRate {
    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "trace_idx", referencedColumnName = "idx")
    private Trace trace;

    private Double rate;
    private Integer traceRate;
    private Double lossRate;
    private Integer sort;

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
