package com.example.application.bybit.trace.entity;

import lombok.*;

import com.example.application.member.*;
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
public class Trace {

    @Id
    @GeneratedValue
    private Integer idx;

    @ManyToOne
    @JoinColumn(name = "member_idx", referencedColumnName = "idx", nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer minuteBong = 1;

    private Integer maxLevel = 0;

    private Double realPrice = 0.0;

    private boolean buyFlag = true;
    private boolean endFlag = false;
    private boolean cancelFlag = false;

    @OneToMany(mappedBy = "trace")
    public List<TraceList> traceLists = new ArrayList<>();

    @OneToMany(mappedBy = "trace")
    public List<TraceBongBaseRate> traceRates = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;
}
