package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.TraceFailLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceFailLogRepository extends JpaRepository<TraceFailLog, Integer> {
}
