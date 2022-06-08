package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraceRepository extends JpaRepository<Trace, Integer> {
    Optional<Trace> findByEndFlagAndMinuteBongAndMember_Idx(boolean end, Integer minuteBong, Integer member_idx);
}
