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
    private List<BongBaseRate> rates = new ArrayList<>();

    @Column(unique = true, nullable = false)
    private Integer minuteBong;

    private Double enterRate;

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

}
