package com.example.application.bybit.trace;

import com.example.application.bybit.dto.response.BybitTrace;
import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.enums.ORDER_STATUS;
import com.example.application.bybit.enums.ORDER_TYPE;
import com.example.application.bybit.enums.SIDE;
import com.example.application.bybit.enums.TIME_IN_FORCE;
import com.example.application.bybit.util.BybitOrderUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class TraceHandler extends TextWebSocketHandler {
    Trace trace;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TraceService traceService;
    private final String secretKey;
    private final String apiKey;
    private final Integer memberIdx;
    private final Integer minuteBong;

    public TraceHandler (
            TraceService traceService,
            String secretKey,
            String apiKey,
            Integer idx,
            Integer minuteBong

    ) {
        super();

        this.traceService = traceService;
        this.secretKey = secretKey;
        this.apiKey = apiKey;
        this.memberIdx = idx;
        this.minuteBong = minuteBong;

        var optionalTrace = traceService.dataSet(memberIdx, minuteBong);

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
            var optionalTrace = traceService.dataSet(memberIdx, minuteBong);

            if (optionalTrace.isEmpty()) {
                // Slack 알람
                throw new RuntimeException("확인할 데이터가 없습니다.");
            }

            trace = optionalTrace.get();
        }

        traceService.traceDataStart(session);

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

            // 실시간 데이터
            var bybitTrace = objectMapper.readValue(message.getPayload(), BybitTrace.class);

            log.info("objectMapper: " + bybitTrace.toString());

            // 현재 가격
            var nowPrice = bybitTrace.getData().get(0).getPrice();

            // DB 에서 조회한 데이터
            var traceLists = trace.getTraceLists();

            // 구매 데이터가 없는 경우 || 구매 데이터가 더 많은 경우
            if (traceLists.stream().filter(traceList -> traceList.getLevel().equals(0)).count() != 1) {
                // Slack 알람

                close(session);
                throw new RuntimeException("데이터가 이상합니다. (구매 기준 데이터는 한개만 존재해야함)");
            }

            // trace Level 정렬
            trace.setTraceLists(
                    trace.getTraceLists()
                            .stream()
                            .sorted(Comparator.comparing(TraceList::getLevel))
                            .collect(Collectors.toList())
            );

            // Level 갯수만큼 반복
            for (int i = 1; i <= trace.getMaxLevel(); i++) {

                // 지정가가 무조건 한개만 있어야하는데 데이터가 없거나 여러 개인경우 에러
                int nowIdx = i;
                if (traceLists.stream().filter(traceList -> traceList.getLevel().equals(nowIdx) && traceList.getOrderType().equals(ORDER_TYPE.Limit)).count() != 1) {
                    close(session);
                    throw new RuntimeException("데이터가 이상합니다. ( [" + i + "]번째 지정가 데이터는 한개만 존재해야함)");
                }

                // true 공매수, false 공매도
                boolean isBuy = trace.isBuyFlag();

                // 구매한 데이터 가져옴
                TraceList traceList = trace.getTraceLists().get(nowIdx);

                if (isBuy ? traceList.getStopLossPrice() >= nowPrice : traceList.getStopLossPrice() <= nowPrice) {

                    // 손절 - 거래 취소 후 모든 금액 팔기 [거래 종료]
                    stopLoss(session, traceLists);

                    close(session);

                }

                if (traceList.isOk()) {

                } else if (isBuy ? traceList.getPrice() <= nowPrice : traceList.getPrice() >= nowPrice) {

                    // 정상적으로 판매가 되었는지 확인하는 과정이 있어야함
                    var traceListParam = traceLists
                            .stream()
                            .filter(traceData -> traceData.getLevel().equals(nowIdx) && traceData.getOrderType().equals(ORDER_TYPE.Limit))
                            .collect(Collectors.toList())
                            .get(0);

                    TraceList traceData = traceService.traceListDataUpdate(traceListParam, apiKey, secretKey);
                    trace.getTraceLists().remove(traceData);
                    trace.getTraceLists().add(traceData);

                    if (i == trace.getMaxLevel()) {
                        traceService.end(trace.getIdx());

                        close(session);
                    }
                } else {
                    break;
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

        // 1. (ORDER_STATUS - Filled 가 아닌 것)
        // 모두 취소하기위해 데이터를 가져옴
        Stream<TraceList> filterStream = traceLists.stream().filter(
                traceList -> traceList.getOrderType().equals(ORDER_TYPE.Limit)
                        && !(traceList.getOrderStatus().equals(ORDER_STATUS.Filled))
        );

        // 가져온 데이터를 이용하여 Bybit Api 호출 (실질적으로 취소)
        filterStream.forEach(
                traceData -> {
                    ResponseEntity<?> responseEntity = BybitOrderUtil.order_cancel(apiKey, secretKey, traceData.getOrderLinkId());
                    if (responseEntity == null || (!HttpStatus.OK.equals(responseEntity.getStatusCode())) ){

                        // 주문취소 실패시 Slack 알림
                    }
                }
        );

        // 2. 데이터 조회 후 값 Update
        trace = traceService.traceCancelUpdate(trace, apiKey, secretKey);

        // 3. 시장가 bybit
        // TODO: ERROR 처리 고려
        BybitOrderUtil.order(
                apiKey,
                secretKey,

                // TODO 확인해봐야함
                0, // 포지션 size qty - ( 내가 가진 비트 코인 * 현재가 )
                                        // /v2/private/wallet/balance
                                        // available_balance

                trace.isBuyFlag() ? SIDE.Sell : SIDE.Buy,
                TIME_IN_FORCE.GoodTillCancel,
                0,
                ORDER_TYPE.Market
        );

        // 4. 테이블 완료 처리
        traceService.end(trace.getIdx());

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
