package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.*;
import com.example.application.bybit.trace.repository.*;
import com.example.application.bybit.trace.dto.response.BybitOrder;
import com.example.application.bybit.trace.dto.response.BybitOrderData;
import com.example.application.bybit.trace.enums.*;
import com.example.application.bybit.util.BybitOrderUtil;
import com.example.application.member.Member;
import com.example.application.member.MemberApi;
import com.example.application.member.MemberApiRepository;
import com.example.application.member.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
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

    @Transactional
    public List<Trace> commonTraceSet(Integer minuteBong, Double price) {

        var resultList = new ArrayList<Trace>();

        var members = memberRepository.findByTraceYn("Y");
        if (members.size() > 0 ) {

            // Api Key, Secret Key 출력
            var memberApis
                    = memberApiRepository.findByMinuteBongAndMemberIdxIn (
                            minuteBong,
                            members.stream().map(Member::getIdx).collect(Collectors.toList())
                    );

            if (memberApis.size() > 0) {

                // Trace Rate 기준 데이터 가져오기
                var traceRateOptional =  traceBaseRateRepository.findByMinuteBong(minuteBong);
                if (traceRateOptional.isEmpty()){

                    // Slack 알람
                    return resultList;
                }

                // 예 - [5분봉] Level 여러개 0.618,0.786...
                var traceRate = traceRateOptional.get();

                // Bong 퍼센트 기준 데티터 가져오기
                var bongBaseOptional =  bongBaseRepository.findByMinuteBong(minuteBong);
                if (bongBaseOptional.isEmpty()){

                    // Slack 알람
                    return resultList;
                }

                // TODO Bybit 현재 실거래가격을 확인 [0.5 거래, Buy Sell 잘 구분해야함]
                // logPrice 비교해서 다른 경우만 진행

                // TODO: Bybit 나의 주문 리스트를 가져온 뒤 주문이 안된 리스트는 취소
                // ex) Sell 이면 Sell 데이터만 날려야함 Sell -> Buy

                var bongBase = bongBaseOptional.get();


                // 저점, 고점
                var basePrice = traceRate.getBasePrice();

                // 매수,매도 여부
                var isBuy = traceRate.isBuyFlag();

                // [목표가 구하는 것]
                // Buy  = 현재가 + ( 현재가 - 기준(저점) )
                // Sell = 현재가 - ( 기준(고점) - 현재가 )
                var targetPrice = isBuy ? price + (price - basePrice) : price - (basePrice - price);

                memberApis.forEach(

                        memberApi -> {

                            // TODO Bybit 내가 가진 비트 코인 조회
                            // /v2/private/wallet/balance
                            // available_balance
                            var myQty = 0.0;

                            // TODO Bybit 포지션 조회

                            // TODO Bybit 현재 실거래가격을 확인 [0.5 거래, Buy Sell 잘 구분해야함]
                            var realPrice = 0.0;

                            // 저점 + (목표가 - 저점) * 비율
                            var enterPrice = basePrice + (targetPrice - basePrice) * bongBase.getEnterRate();
                            if ( isBuy ? enterPrice >= realPrice : enterPrice <= realPrice  ) {

                                // TODO: 구매할 수 있는 수량
                                // Buy  = 보유량 - (보유량 + 포지션)
                                // Sell = 보유량 + (보유량 + 포지션)
                                var qty = 0;

                                // 구매 [지정가]
                                var responseEntity = BybitOrderUtil.order(
                                        memberApi.getApiKey(),
                                        memberApi.getSecretKey(),
                                        qty,
                                        isBuy ? SIDE.Buy : SIDE.Sell,
                                        TIME_IN_FORCE.PostOnly,
                                        realPrice,
                                        ORDER_TYPE.Limit
                                );

                                if (responseEntity != null && responseEntity.getStatusCode().equals(HttpStatus.OK)) {



                                    var body = Objects.requireNonNull(responseEntity.getBody()).toString();
                                    var objectMapper = new ObjectMapper();

//                                var bybitOrder = objectMapper.readValue(body, BybitOrder.class);

                                    // 구매 [지정가 내역 임시 저장 - 판매가 내역 생성 안된 시점]
                                    var traceParam = new Trace (
                                            null,
                                            memberApi.getMember(),
                                            minuteBong,
                                            0,
                                            price,
                                            realPrice,
                                            0,
                                            false,
                                            false,
                                            false,
                                            false,
                                            new ArrayList<>(),
                                            new ArrayList<>(),
                                            LocalDateTime.now(),
                                            LocalDateTime.now()
                                    );
                                    var trace = traceRepository.save(traceParam);

                                    // TODO
                                    // 구매한 결과 Save Lv0
                                    var traceBaseListParam = new TraceList(

                                    );
                                    var traceBaseList = traceListRepository.save(traceBaseListParam);
                                    traceBaseList.setTrace(trace);
                                    trace.getTraceLists().add(traceBaseList);

                                    // 실구매된 경우 저장
                                    resultList.add(trace);

                                } else {
                                    // Slack 알림
                                }
                            }
                        }
                );
            }
        }

        return resultList;
    }

    @Transactional
    public List<Trace> commonTraceStart(Integer minuteBong) {

        var resultList = new ArrayList<Trace>();
        var traces = traceRepository.findByStartFlagAndMinuteBong(false, minuteBong);

        var bongBaseOptional =  bongBaseRepository.findByMinuteBong(minuteBong);
        if (bongBaseOptional.isEmpty()){

            // Slack 알람
            return resultList;
        }

        var bongBase = bongBaseOptional.get();

        var rates = bongBase
                .getRates()
                .stream()
                .sorted(Comparator.comparing(BongBaseRate::getSort))
                .collect(Collectors.toList());

        traces.forEach(
                trace -> {

                    var totalQty = trace.getQty();
                    var isBuy  = trace.isBuyFlag();
                    var member = trace.getMember();
                    var memberApiOptional = memberApiRepository.findByMinuteBongAndMemberIdx(minuteBong, member.getIdx());

                    if (memberApiOptional.isPresent()) {

                        var memberApi = memberApiOptional.get();
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

                                    // TODO: 내림으로 비율 계산 해야함
                                    var qty = 0;
                                    if (rate.getSort() == rates.size()) {
                                        qty = 1;
                                    } else {
                                        qty = totalQty;
                                    }

                                    // [지정가 설정]
                                    var response = BybitOrderUtil.order(
                                            memberApi.getApiKey(),
                                            memberApi.getSecretKey(),
                                            qty,
                                            isBuy ? SIDE.Buy : SIDE.Sell,
                                            TIME_IN_FORCE.PostOnly,
                                            0.0,
                                            ORDER_TYPE.Limit
                                    );

                                    if (response != null && response.getStatusCode().equals(HttpStatus.OK)) {

                                        // TODO
                                        // 판매 금액 = Buy -> 저점 + (목표가 - 저점) * 비율
                                        var traceListParam = new TraceList(

                                        );

                                        var traceList = traceListRepository.save(traceListParam);
                                        traceList.setTrace(trace);
                                        trace.getTraceLists().add(traceList);

                                        if (rate.getSort() == rates.size()) {
                                            trace.setStartFlag(true);
                                        }

                                        resultList.add(trace);
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

        return resultList;
    }


    // 초기 데이터 조회
    public Optional<Trace> dataSet(Integer memberIdx, Integer minuteBong){
        // traces Table 조회
        return traceRepository.findByStartFlagAndEndFlagAndMinuteBongAndMember_Idx(
                true,
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
