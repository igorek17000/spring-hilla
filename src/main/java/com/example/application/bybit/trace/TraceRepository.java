package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TraceRepository extends JpaRepository<Trace, Integer> {
    Optional<Trace> findByEndAndMinuteBongAndMember_Idx(boolean end, Integer minuteBong, Integer member_idx);
}
