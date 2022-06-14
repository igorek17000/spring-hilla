package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.TraceRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceRateRepository extends JpaRepository<TraceRate, Integer> {
}
