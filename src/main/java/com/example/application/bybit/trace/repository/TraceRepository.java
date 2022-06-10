package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.Trace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TraceRepository extends JpaRepository<Trace, Integer> {
    Optional<Trace> findByStartFlagAndEndFlagAndMinuteBongAndMember_Idx(boolean startFlag, boolean endFlag, Integer minuteBong, Integer member_idx);
    List<Trace> findByStartFlagAndMinuteBong(boolean startFlag, Integer minuteBong);
}
