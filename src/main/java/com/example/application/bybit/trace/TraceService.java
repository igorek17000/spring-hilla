package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.trace.repository.TraceListRepository;
import com.example.application.bybit.trace.repository.TraceRepository;
import com.example.application.bybit.dto.response.BybitOrder;
import com.example.application.bybit.dto.response.BybitOrderData;
import com.example.application.bybit.enums.*;
import com.example.application.bybit.util.BybitOrderUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceListRepository traceListRepository;

    private List<ORDER_STATUS> searchOrderType = List.of(ORDER_STATUS.PendingCancel, ORDER_STATUS.PartiallyFilled, ORDER_STATUS.Created);

    // 초기 데이터 조회
    public Optional<Trace> dataSet(Integer memberIdx, Integer minuteBong){
        // traces Table 조회
        return traceRepository.findByEndFlagAndMinuteBongAndMember_Idx(
                false,
                minuteBong,
                memberIdx
        );
    }

    @Transactional
    public void end( Integer idx ) {
        var traceOptional = traceRepository.findById(idx);
        traceOptional.ifPresent(trace -> trace.setEndFlag(true));
    }


    @Transactional
    public Trace traceCancelUpdate(Trace traceParam, String apiKey, String secretKey) {

        // DB Filled 제외하고 조회
        var traceLists = traceListRepository.findByOrderTypeAndOrderStatusNotAndTrace_Idx(ORDER_TYPE.Limit, ORDER_STATUS.Filled, traceParam.getIdx());

        if (traceLists.size() == 0) {
            return traceParam;
        }

        // 나의 주문리스트를 조회 (Bybit Api) - 이거 이전에 취소 Api를 호출했는데 진짜 취소되었는지 대조하기위한 작업
        var responseEntity = BybitOrderUtil.order_list(
                apiKey,
                secretKey,
                ORDER_STATUS.Cancelled.toString()
        );

        // 통신 에러
        if (responseEntity == null || (!responseEntity.getStatusCode().equals(HttpStatus.OK))) {
            // slack 알림 전송

            return traceParam;

        } else {

            String body = Objects.requireNonNull(responseEntity.getBody()).toString();
            ObjectMapper objectMapper = new ObjectMapper();

            try {
                var bybitOrder = objectMapper.readValue(body, BybitOrder.class);

                // Bybit에서 조회한 데이터 (주문번호만 추출)=
                var bybitOrderIdLists = bybitOrder.getResult()
                        .getData()
                        .stream()
                        .map(BybitOrderData::getOrder_link_id)
                        .collect(Collectors.toList());

                if (bybitOrder.getRet_msg().equals("OK")) {

                    // DB 데이터
                    traceLists.forEach(
                            traceData -> {
                                    // DB 데이터가 Bybit에서 조회한 데이터에 포함이된다면 취소 update query 를 해야 일치화를 시킬 수 있음
                                    if (bybitOrderIdLists.stream().anyMatch(bybitOrderData -> bybitOrderData.equals(traceData.getOrderLinkId()))){
                                        traceData.setOrderStatus(ORDER_STATUS.Cancelled);
                                    }
                            }
                    );


                    Trace trace = traceLists.get(0).getTrace();
                    // trace 에 trace List 가 모두 취소되었을때 Master 테이블에 isCancel true update query
                    if (traceLists.stream().allMatch(traceData -> traceData.getOrderStatus().equals(ORDER_STATUS.Cancelled))){
                        trace.setCancelFlag(true);
                    }
                    return trace;
                }
            } catch (JsonProcessingException e) {

                // Slack 알림
            }

            return traceParam;
        }
    }

    @Transactional
    public TraceList traceListDataUpdate (
            TraceList traceListParam,
            String apiKey,
            String secretKey
    ) {

        var traceListOptional = traceListRepository.findById(traceListParam.getIdx());
        if (traceListOptional.isEmpty()) {
            return traceListParam;
        }

        var responseEntity = BybitOrderUtil.order_list(
                apiKey,
                secretKey,
                ORDER_STATUS.Filled.toString()
        );

        if (responseEntity == null || (!responseEntity.getStatusCode().equals(HttpStatus.OK))) {
            // slack 알림 전송
            return traceListParam;
        } else {
            String body = Objects.requireNonNull(responseEntity.getBody()).toString();

            ObjectMapper objectMapper = new ObjectMapper();
            try {
                var bybitOrder = objectMapper.readValue(body, BybitOrder.class);

                var bybitOrderLists = bybitOrder.getResult()
                        .getData()
                        .stream()
                        .map(BybitOrderData::getOrder_link_id)
                        .collect(Collectors.toList());

                if (bybitOrder.getRet_msg().equals("OK")) {
                    var traceList = traceListOptional.get();
                    if (bybitOrderLists.stream().anyMatch(bybitOrderData -> bybitOrderData.equals(traceList.getOrderLinkId()))){
                        traceList.setOrderStatus(ORDER_STATUS.Filled);
                    }
                    return traceList;
                }


            } catch (JsonProcessingException e) {

                e.printStackTrace();

                // Slack 알림
            }

            return traceListParam;
        }
    }

    public void traceDataStart(WebSocketSession session) {

        // {"op":"unsubscribe","args":["trade.BTCUSD"]}
        // {"op":"subscribe","args":["trade.BTCUSD"]}
        try {
            session.sendMessage(new TextMessage("{\"op\":\"subscribe\",\"args\":[\"trade.BTCUSD\"]}"));
        } catch (IOException e) {

            // Slack 알람 전송

            throw new RuntimeException(e);
        }

        // 조회 결과
        // {
        //    "topic": "trade.BTCUSD",
        //    "data": [
        //        {
        //            "timestamp": "2020-01-12T16:59:59.000Z",
        //            "trade_time_ms": 1582793344685,
        //            "symbol": "BTCUSD",
        //            "side": "Sell",
        //            "size": 328,
        //            "price": 8098,
        //            "tick_direction": "MinusTick",
        //            "trade_id": "00c706e1-ba52-5bb0-98d0-bf694bdc69f7",
        //            "cross_seq": 1052816407
        //        }
        //    ]
        // }
        // 주의점
        // 1. https://bybit-exchange.github.io/docs/inverse/#price-price 확인
    }


}
