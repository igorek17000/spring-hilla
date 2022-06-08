package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceMinuteBongBase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraceMinuteBongBaseRepository extends JpaRepository<TraceMinuteBongBase, Integer> {
}
