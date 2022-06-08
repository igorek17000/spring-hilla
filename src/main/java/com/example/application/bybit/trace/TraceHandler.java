package com.example.application.bybit.trace;

import com.example.application.bybit.BybitService;
import com.example.application.bybit.trace.bybit.BybitTrace;
import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.trace.enums.ORDER_STATUS;
import com.example.application.bybit.trace.enums.ORDER_TYPE;
import com.example.application.bybit.trace.enums.SIDE;
import com.example.application.bybit.trace.enums.TIME_IN_FORCE;
import com.example.application.bybit.util.OrderUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class TraceHandler extends TextWebSocketHandler {
    Trace trace;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final BybitService bybitService;
    private final String secretKey;
    private final String apiKey;
    private final Integer memeberIdx;
    private final Integer minuteBong;

    public TraceHandler (
            BybitService bybitService,
            String secretKey,
            String apiKey,
            Integer idx,
            Integer minuteBong

    ) {
        super();

        this.bybitService = bybitService;
        this.secretKey = secretKey;
        this.apiKey = apiKey;
        this.memeberIdx = idx;
        this.minuteBong = minuteBong;

        var optionalTrace = bybitService.dataSet(memeberIdx, minuteBong);

        if (optionalTrace.isEmpty()) {
            // Slack 알람
            throw new RuntimeException("확인할 데이터가 없습니다.");
        }

        trace = optionalTrace.get();
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {

        log.info("afterConnectionEstablished ID: " + session.getId());

        // TODO: 처리해야하는 것
        // Timer 이용
        // 1. 하트비트 패킷 을 전송하여 연결 유지 ( ping 네트워크 또는 프로그램 문제를 방지하려면 WebSocket 연결을 유지하기 위해 30초마다 하트비트 패킷 을 보내는 것이 좋습니다 . )
        // 2. 연결이 끊긴 경우 최대한 빨리 다시 연결
        // session.sendMessage(new TextMessage("{\"op\":\"ping\"}"));
        if (trace == null) {
            var optionalTrace = bybitService.dataSet(memeberIdx, minuteBong);

            if (optionalTrace.isEmpty()) {
                // Slack 알람
                throw new RuntimeException("확인할 데이터가 없습니다.");
            }

            trace = optionalTrace.get();
        }

        bybitService.traceDataStart(session);

        try {
            super.afterConnectionEstablished(session);
        } catch (Exception e) {

            // Slack 알람 전송
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {

        log.info("handleTextMessage ID: "      + session.getId());
        log.info("handleTextMessage Message: " + message.getPayload() + "(" + message.getPayloadLength() + ")");

        try {

            var bybitTrace = objectMapper.readValue(message.getPayload(), BybitTrace.class);

            log.info("objectMapper: " + bybitTrace.toString());

            // TODO: 확인해야하는 것 (Buy, Sell 이든 금액을 가져오면 되는건지?)
            var nowPrice = bybitTrace.getData().get(0).getPrice();

            var traceLists = trace.getTraceLists();

            // Lv -> 0, 1, 2, 3
            // [0 구매], [1,2,3 지점 판매]
            if (traceLists.stream().filter( traceList -> traceList.getLevel().equals(0) && traceList.getOrderType().equals(ORDER_TYPE.Limit)).count() == 1) {
                // Slack 알람

                close(session);
                throw new RuntimeException("데이터가 이상합니다. (구매기준 지정가 데이터는 한개만 존재해야함)");
            }

            if (traceLists.stream().filter( traceList -> traceList.getLevel().equals(1) && traceList.getOrderType().equals(ORDER_TYPE.Limit)).count() == 1) {
                // Slack 알람

                close(session);
                throw new RuntimeException("데이터가 이상합니다. (첫번째 지정가 데이터는 한개만 존재해야함)");
            }

            if (traceLists.stream().filter( traceList -> traceList.getLevel().equals(2) && traceList.getOrderType().equals(ORDER_TYPE.Limit)).count() == 1) {
                // Slack 알람

                close(session);
                throw new RuntimeException("데이터가 이상합니다. (두번째 지정가 데이터는 한개만 존재해야함)");
            }

            if (traceLists.stream().filter( traceList -> traceList.getLevel().equals(3) && traceList.getOrderType().equals(ORDER_TYPE.Limit)).count() == 1) {
                // Slack 알람

                close(session);
                throw new RuntimeException("데이터가 이상합니다. (세번째 지정가 데이터는 한개만 존재해야함)");
            }

            var stopLossPrice = trace.getStopLossPrice(); /* 손절 가격  */
            var basePrice     = trace.getPrice();         /* 구매가격   */
            var onePrice      = trace.getOnePrice();      /* 1차 판매가격*/
            var twoPrice      = trace.getTwoPrice();      /* 2차 판매가격*/
            var threePrice    = trace.getThreePrice();    /* 3차 판매가격*/

            boolean isBuy = trace.isBuy();

            if (isBuy ? stopLossPrice >= nowPrice : stopLossPrice <= nowPrice) {

                // 손절 - 거래 취소 후 모든 금액 팔기 [거래 종료]
                stopLoss(session, traceLists);

            } else if (trace.getOneOk().equals("N")) {

                if (isBuy ? onePrice <= nowPrice : onePrice >= nowPrice) {

                    // 구매되었는지 확인 Lv1
                    // 구매되었으면 테이블 Update 처리, List 해당 trace 에 oneYn Update
                    var traceListParam = traceLists
                            .stream()
                            .filter(traceList -> traceList.getLevel().equals(1) && traceList.getOrderType().equals(ORDER_TYPE.Limit))
                            .collect(Collectors.toList())
                            .get(0);

                    TraceList traceList = bybitService.traceListDataUpdate(trace, traceListParam, apiKey, secretKey,1);
                    trace.getTraceLists().remove(traceListParam);
                    trace.getTraceLists().add(traceList);

                }

            } else if ( trace.getOneOk().equals("Y") && trace.getTwoOk().equals("N") ) {

                if ( isBuy ? basePrice >= nowPrice : basePrice <= nowPrice ) {

                    // 손절 - 거래 취소 후 나머지 50퍼 금액 팔기 [거래 종료]
                    stopLoss(session, traceLists);

                } else if(isBuy ? twoPrice >= nowPrice : twoPrice <= nowPrice) {
                    var traceListParam = traceLists
                            .stream()
                            .filter(traceList -> traceList.getLevel().equals(2) && traceList.getOrderType().equals(ORDER_TYPE.Limit))
                            .collect(Collectors.toList())
                            .get(0);

                    TraceList traceList = bybitService.traceListDataUpdate(trace, traceListParam, apiKey, secretKey,2);
                    trace.getTraceLists().remove(traceListParam);
                    trace.getTraceLists().add(traceList);
                }

            } else {

                if ( isBuy ? onePrice >= nowPrice : onePrice <= nowPrice ) {

                    // 손절 - 거래 취소 후 나머지 25퍼 금액 팔기 [거래 종료]
                    stopLoss(session, traceLists);

                    close(session);

                } else if(isBuy ? threePrice >= nowPrice : threePrice <= nowPrice) {

                    // 구매되었는지 조회 Lv3
                    // 구매되었으면 테이블 Update 처리, List 해당 trace 에 threeYn Update
                    // [거래 종료]
                    var traceListParam = traceLists
                            .stream()
                            .filter(traceList -> traceList.getLevel().equals(3) && traceList.getOrderType().equals(ORDER_TYPE.Limit))
                            .collect(Collectors.toList())
                            .get(0);

                    TraceList traceList = bybitService.traceListDataUpdate(trace, traceListParam, apiKey, secretKey,3);
                    trace.getTraceLists().remove(traceListParam);
                    trace.getTraceLists().add(traceList);
                    trace.setEnd(true);

                    // Slack 알람 전송

                    close(session);
                }
            }

        } catch (JsonProcessingException e) {

            // Slack 알람 전송

            e.printStackTrace();
        }

        try {
            super.handleTextMessage(session, message);
        } catch (Exception e) {

            // Slack 알람 전송
            throw new RuntimeException(e);
        }
    }



    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {

        // Slack 알람 전송

        log.info("handleTransportError ID: " + session.getId());
        exception.printStackTrace();

        super.handleTransportError(session, exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {

        // Slack 알람 전송

        log.info("handleTransportError ID: " + session.getId());
        log.info("afterConnectionClosed Reason: " + status.getReason());

        super.afterConnectionClosed(session, status);
    }

    private void stopLoss(WebSocketSession session, List<TraceList> traceLists) {
        // 1. 1차 지정가 제외 지정가 내역 취소 (ORDER_STATUS - New 가 아닌 것)
        Stream<TraceList> filterStream = traceLists.stream().filter(
                traceList -> traceList.getOrderType().equals(ORDER_TYPE.Limit) && !traceList.getOrderStatus().equals(ORDER_STATUS.New)
        );
        filterStream.forEach(
                traceData -> {
                    ResponseEntity<?> responseEntity = OrderUtil.order_cancel(apiKey, secretKey, traceData.getOrderLinkId());
                    if (responseEntity == null || (!HttpStatus.OK.equals(responseEntity.getStatusCode())) ){
                        // 주문취소 실패시 Slack 알림
                    }
                }
        );

        // 2. 데이터 조회 후 값 Update
        trace = bybitService.traceCancelUpdate(trace, apiKey, secretKey);

        // 3. 시장가 bybit
        // TODO: ERROR 처리 고려
        Integer totalQty = trace.getTotalQty();
        OrderUtil.order(
                apiKey,
                secretKey,
                totalQty - (totalQty - filterStream.map(TraceList::getQty).reduce(0, Integer::sum)),
                trace.isBuy() ? SIDE.Sell : SIDE.Buy,
                TIME_IN_FORCE.GoodTillCancel,
                0,
                ORDER_TYPE.Market
        );

        // 4. 테이블 완료 처리
        bybitService.end(trace.getIdx());

        // Slack 알람 전송


        close(session);
    }

    private void close(WebSocketSession session) {
        try {
            session.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }



}
