package com.example.application.bybit.trace.repository;

import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.enums.ORDER_STATUS;
import com.example.application.bybit.enums.ORDER_TYPE;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TraceListRepository extends JpaRepository<TraceList, Integer> {

    List<TraceList> findByOrderTypeAndOrderStatusNotAndTrace_Idx(ORDER_TYPE orderType, ORDER_STATUS orderStatus, Integer trace_idx);

}