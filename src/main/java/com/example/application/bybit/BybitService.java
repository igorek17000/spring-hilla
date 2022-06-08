package com.example.application.bybit;

import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.trace.TraceListRepository;
import com.example.application.bybit.trace.TraceRepository;
import com.example.application.bybit.trace.bybit.BybitOrder;
import com.example.application.bybit.trace.bybit.BybitOrderData;
import com.example.application.bybit.trace.enums.ORDER_STATUS;
import com.example.application.bybit.trace.enums.ORDER_TYPE;
import com.example.application.bybit.util.OrderUtil;
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
public class BybitService {

    private final TraceRepository traceRepository;
    private final TraceListRepository traceListRepository;
    private List<ORDER_STATUS> searchOrderType = List.of(ORDER_STATUS.PendingCancel, ORDER_STATUS.PartiallyFilled, ORDER_STATUS.Created);

    // 초기 데이터 조회
    public Optional<Trace> dataSet(Integer memberIdx, Integer minuteBong){
        // traces Table 조회
        return traceRepository.findByEndAndMinuteBongAndMember_Idx(
                false,
                minuteBong,
                memberIdx
        );
    }

    @Transactional
    public void end( Integer idx ) {
        var traceOptional = traceRepository.findById(idx);
        traceOptional.ifPresent(trace -> trace.setEnd(true));
    }


    @Transactional
    public Trace traceCancelUpdate(Trace traceParam, String apiKey, String secretKey) {

        var traceLists = traceListRepository.findByOrderTypeAndOrderStatusNotAndTrace_Idx(ORDER_TYPE.Limit, ORDER_STATUS.New, traceParam.getIdx());
        if (traceLists.size() == 0) {
            return traceParam;
        }

        var responseEntity = OrderUtil.order_list(
                apiKey,
                secretKey,
                ORDER_STATUS.Cancelled.toString()
        );

        if (responseEntity == null || (!responseEntity.getStatusCode().equals(HttpStatus.OK))) {
            // slack 알림 전송

            return traceParam;
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
                    traceLists.forEach(
                            traceData -> {
                                    if (bybitOrderLists.stream().anyMatch(bybitOrderData -> bybitOrderData.equals(traceData.getOrderLinkId()))){
                                        traceData.setOrderStatus(ORDER_STATUS.Cancelled);
                                    }
                            }
                    );

                    Trace trace = traceLists.get(0).getTrace();
                    if (traceLists.stream().allMatch(traceData -> traceData.getOrderStatus().equals(ORDER_STATUS.Cancelled))){
                        trace.setCancel(true);
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
    public TraceList traceListDataUpdate(Trace traceParam ,TraceList traceListParam, String apiKey, String secretKey, Integer lv) {

        var traceListOptional = traceListRepository.findById(traceListParam.getIdx());
        if (traceListOptional.isEmpty()) {
            return traceListParam;
        }

        var responseEntity = OrderUtil.order_list(
                apiKey,
                secretKey,
                ORDER_STATUS.New.toString()
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
                        traceList.setOrderStatus(ORDER_STATUS.New);

                        var traceOptional = traceRepository.findById(traceParam.getIdx());

                        if (lv.equals(1)) {
                            traceOptional.ifPresent(value -> value.setOneOk("Y"));
                        } else if (lv.equals(2)) {
                            traceOptional.ifPresent(value -> value.setTwoOk("Y"));
                        } else if (lv.equals(3)) {
                            if (traceOptional.isPresent()){
                                Trace trace = traceOptional.get();
                                trace.setEnd(true);
                            }
                        }
                    }
                    return traceList;
                }


            } catch (JsonProcessingException e) {

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
