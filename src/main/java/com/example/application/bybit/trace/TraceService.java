package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.BongBaseRate;
import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.entity.TraceBongBaseRate;
import com.example.application.bybit.trace.entity.TraceList;
import com.example.application.bybit.trace.repository.*;
import com.example.application.bybit.trace.dto.response.BybitOrder;
import com.example.application.bybit.trace.dto.response.BybitOrderData;
import com.example.application.bybit.trace.enums.*;
import com.example.application.bybit.util.BybitOrderUtil;
import com.example.application.member.Member;
import com.example.application.member.MemberApiRepository;
import com.example.application.member.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceListRepository traceListRepository;
    private final TraceBaseRateRepository traceBaseRateRepository;
    private final TraceBongBaseRateRepository traceBongBaseRateRepository;
    private final MemberRepository memberRepository;
    private final MemberApiRepository memberApiRepository;

    private final BongBaseRepository bongBaseRepository;

    // 거래중이지 않은 회원 기준으로 BaseRate 를 이용하여 구매
    public boolean common_trace(Integer minuteBong, Double price) {

        // 거래중인 내역 가져오기
        var traces = traceRepository.findByEndFlagAndMinuteBong(
                false,
                minuteBong
        );

        // 거래중인 회원 리스트
        var traceMembersStream = traces.stream().map(Trace::getMember);

        // 거래중이지 않은 회원 리스트
        var notTraceMembers = memberRepository.findByIdxNotInAndTraceYn(
                traceMembersStream.map(Member::getIdx).collect(Collectors.toList())
                , "N"
        );

        if (notTraceMembers.size() > 0 ) {

            // Api Key, Secret Key 출력
            var memberApis
                    = memberApiRepository.findByMinuteBongAndMemberIdxIn(
                            minuteBong,
                            notTraceMembers.stream().map(Member::getIdx).collect(Collectors.toList())
                    );

            if (memberApis.size() > 0) {

                // Trace Rate 기준 데이터 가져오기
                var traceRateOptional =  traceBaseRateRepository.findByMinuteBong(minuteBong);
                if (traceRateOptional.isEmpty()){
                    // Slack 알람
                    return false;
                }

                // Bong 퍼센트 기준 데티터 가져오기
                var bongBaseOptional =  bongBaseRepository.findByMinuteBong(minuteBong);
                if (bongBaseOptional.isEmpty()){
                    // Slack 알람
                    return false;
                }

                var rates = bongBaseOptional.get()
                        .getRates()
                        .stream()
                        .sorted(Comparator.comparing(BongBaseRate::getSort))
                        .collect(Collectors.toList());

                var traceRate = traceRateOptional.get();

                // 목표가
                var targetPrice = traceRate.getTargetPrice();

                // 매수,매도 여부
                var isBuy = traceRate.isBuyFlag();

                // 저점
                // Buy  = 현재가 - (목표가 - 현재가)
                // Sell = 현재가 + (현재가 - 목표가)
                var lowPrice = isBuy ? price - (targetPrice - price) : price + (price - targetPrice);

                memberApis.forEach(
                        memberApi -> {

                            // TODO: 구매할 수 있는 수량
                            // 포지션 size qty - ( 내가 가진 비트 코인 * 현재가 )
                            var qty = 0; // 포지션 size qty - ( 내가 가진 비트 코인 * 현재가 )

                            // 실거래금액
                            var realPrice = 0.0;

                            var traceParam = new Trace (
                                    null,
                                    memberApi.getMember(),
                                    minuteBong,
                                    rates.size(),
                                    realPrice,
                                    false,
                                    false,
                                    false,
                                    new ArrayList<>(),
                                    new ArrayList<>(),
                                    LocalDateTime.now(),
                                    LocalDateTime.now()
                            );

                            var trace = traceRepository.save(traceParam);

                            // 구매 [시장가]
                            ResponseEntity<?> responseEntity = BybitOrderUtil.order(
                                    memberApi.getApiKey(),
                                    memberApi.getSecretKey(),
                                    qty,
                                    isBuy ? SIDE.Buy : SIDE.Sell,
                                    TIME_IN_FORCE.GoodTillCancel,
                                    0,
                                    ORDER_TYPE.Market
                            );

                            if (responseEntity != null && responseEntity.getStatusCode().equals(HttpStatus.OK)) {

                                // TODO
                                // 구매한 결과 Save Lv0
                                // 판매 금액 = (목표가 - 현재가) * 비율
                                var traceBaseListParam = new TraceList(

                                );
                                var traceBaseList = traceListRepository.save(traceBaseListParam);
                                traceBaseList.setTrace(trace);
                                trace.getTraceLists().add(traceBaseList);


                                rates.forEach(
                                        rate -> {

                                            // 사용했던 기준 저장
                                            var traceBongBaseRateParam = new TraceBongBaseRate(
                                                    null, null, rate.getRate(), rate.getTraceRate(), rate.getLossRate(),
                                                    rate.getSort(), LocalDateTime.now(), LocalDateTime.now()
                                            );
                                            var traceBongBaseRate = traceBongBaseRateRepository.save(traceBongBaseRateParam);
                                            traceBongBaseRate.setTrace(trace);
                                            trace.getTraceRates().add(traceBongBaseRate);


                                            // [지정가 설정]
                                            ResponseEntity<?> response = BybitOrderUtil.order(
                                                    memberApi.getApiKey(),
                                                    memberApi.getSecretKey(),
                                                    qty,
                                                    isBuy ? SIDE.Buy : SIDE.Sell,
                                                    TIME_IN_FORCE.GoodTillCancel,
                                                    0,
                                                    ORDER_TYPE.Market
                                            );

                                            if (response != null && response.getStatusCode().equals(HttpStatus.OK)) {

                                                // TODO
                                                // 판매 금액 = (목표가 - 현재가) * 비율
                                                var traceListParam = new TraceList(

                                                );

                                                var traceList = traceListRepository.save(traceListParam);
                                                traceList.setTrace(trace);
                                                trace.getTraceLists().add(traceList);

                                            } else {
                                                // Slack 알림
                                            }
                                        }
                                );
                            } else {
                                // Slack 알림
                            }
                        }
                );
            }
        }

        return false;
    }

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

            var body = Objects.requireNonNull(responseEntity.getBody()).toString();
            var objectMapper = new ObjectMapper();

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


                    var trace = traceLists.get(0).getTrace();
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
            var body = Objects.requireNonNull(responseEntity.getBody()).toString();

            var objectMapper = new ObjectMapper();
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
