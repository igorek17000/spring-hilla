package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.BongBaseExitRate;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BongBaseRateRepository extends JpaRepository<BongBaseExitRate, Integer> {
}
