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

    private Integer maxLevel = 0;

    private boolean isBuy = true;
    private boolean isEnd = false;
    private boolean isCancel = false;

    @OneToMany(mappedBy = "trace")
    public List<TraceList> traceLists = new ArrayList<>();

}
