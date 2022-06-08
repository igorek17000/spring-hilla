package com.example.application.bybit.trace.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TraceMinuteBongBase {
    @Id
    @GeneratedValue
    private Integer idx;

    private Integer bong;

    @OneToMany(mappedBy = "traceMinuteBongBase")
    public List<TraceMinuteBongBaseRate> rates = new ArrayList<>();

}
