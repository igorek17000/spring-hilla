package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.TraceExit;
import com.example.application.bybit.trace.enums.ORDER_STATUS;
import com.example.application.bybit.trace.enums.ORDER_TYPE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TraceExitRepository extends JpaRepository<TraceExit, Integer> {

    List<TraceExit> findByOrderTypeAndOrderStatusNotAndTrace_Idx(ORDER_TYPE orderType, ORDER_STATUS orderStatus, Integer trace_idx);

    Optional<TraceExit> findByOrderId(String order_id);
}
