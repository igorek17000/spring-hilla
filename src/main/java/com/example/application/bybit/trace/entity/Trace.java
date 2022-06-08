package com.example.application.bybit.trace.entity;

import lombok.*;

import com.example.application.member.*;

import javax.persistence.*;
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

    private boolean buyFlag = true;
    private boolean endFlag = false;
    private boolean cancelFlag = false;

    @OneToMany(mappedBy = "trace")
    public List<TraceList> traceLists = new ArrayList<>();

    @OneToMany(mappedBy = "trace")
    public List<TraceList> traceRates = new ArrayList<>();
}
