package com.example.application.bybit.trace.entity;

import com.example.application.bybit.member.Member;
import lombok.*;

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
    @JoinColumn(name = "member_idx", referencedColumnName = "id")
    @Column(nullable = false)
    private Member member;

    @Column(nullable = false)
    private Integer minuteBong = 1;

    private Double  price = 0.0;
    private Double  onePrice = 0.0;
    private Double  twoPrice = 0.0;
    private Double  threePrice = 0.0;

    private Integer totalQty = 0;

    private String  oneOk = "N";
    private String  twoOk = "N";

    private Double  stopLossPrice = 0.0;
    private boolean isBuy = true;
    private boolean isEnd = false;
    private boolean isCancel = false;

    @OneToMany(mappedBy = "trace")
    public List<TraceList> traceLists = new ArrayList<>();

}
