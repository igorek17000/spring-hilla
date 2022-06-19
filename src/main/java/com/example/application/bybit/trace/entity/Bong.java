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
public class Bong {

    @Id
    @GeneratedValue
    private Integer idx;

    @Column(unique = true, nullable = false)
    private Integer minuteBong;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Double openPrice; /* 시가 */
    private Double highPrice; /* 최고가 */
    private Double lowPrice; /* 최저가 */
    private Double closePrice; /* 종가 */

    private Double enterPrice; /* 진입금액 */

    private boolean isBuy;

    @CreationTimestamp
    private LocalDateTime createDate;

    @UpdateTimestamp
    private LocalDateTime updateDate;

}
