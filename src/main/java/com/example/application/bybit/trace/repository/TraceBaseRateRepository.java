package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.TraceBaseRate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface TraceBaseRateRepository extends JpaRepository<TraceBaseRate, Integer> {

    Optional<TraceBaseRate> findByMinuteBong(Integer minuteBong);
}
