package com.example.application.bybit.trace;

import com.example.application.bybit.common.SlackNotificationLog;
import com.example.application.bybit.common.SlackNotificationLogRepository;
import com.example.application.bybit.common.SlackNotificationRepository;
import com.example.application.bybit.trace.dto.response.*;
import com.example.application.bybit.trace.entity.*;
import com.example.application.bybit.trace.repository.*;
import com.example.application.bybit.trace.enums.*;
import com.example.application.bybit.util.BybitOrderUtil;
import com.example.application.bybit.util.SlackNotificationUtil;
import com.example.application.member.Member;
import com.example.application.member.MemberApiRepository;
import com.example.application.member.MemberRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

// TODO 전체 실패 로그 DB에 저장, 슬랙 알림 보내기
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceExitRepository traceExitRepository;
    private final TraceEnterRepository traceEnterRepository;
    private final TraceRateRepository traceRateRepository;
    private final MemberRepository memberRepository;
    private final MemberApiRepository memberApiRepository;
    private final BongBaseRepository bongBaseRepository;
    private final SlackNotificationRepository slackNotificationRepository;
    private final SlackNotificationLogRepository slackNotificationLogRepository;


    private final String targetSetRequestPath = "/trace/target/set";
    private final String exitSetRequestPath   = "/exit/set";
    private final String exitSetMethodName    = "traceExitSet";
    private final String targetSetMethodName  = "traceTargetSet";

    /**
     * 진입 금액 세팅
     * @param minuteBong 봉
     * @param price 진입 금액 (완전 현재가 아님)
     * @param isBuy 매수,매도 여부
     * @param basePrice 고점, 저점
     * @return List<Trace>
     */
    @Transactional
    public List<Trace> traceTargetSet(
            Integer minuteBong,
            Double price,
            boolean isBuy,
            Double basePrice
    ) {

        log.info("1. resultList: 결과값 Return 해주기 위한 변수");
        var resultList = new ArrayList<Trace>();

        log.info("Slack 알림 설정을 위한 세팅");
        var objectMapper = new ObjectMapper();
        var slackNotificationOptional = slackNotificationRepository.findById(1);
        if (slackNotificationOptional.isEmpty()){
            return resultList;
        }
        var slackNotification = slackNotificationOptional.get();

        log.info("2. members: 비트코인 거래중인 회원 데이터 모두 불러오기");
        var members = memberRepository.findByTraceYn("Y");
        if (members.size() > 0 ) {

            log.info("3. memberApis: Api Key, Secret Key 출력 (봉기준으로 회원의 모든 ApiKey 가져오기)");
            log.info("4. memberApis Param: 비트코인 거래중인 데이터만 사용");
            var memberApis
                    = memberApiRepository.findByMinuteBongAndMemberIdxIn (
                            minuteBong,
                            // 3-1
                            members.stream().map(Member::getIdx).collect(Collectors.toList())
                    );

            if (memberApis.size() > 0) {

                log.info("5. 진입 기준 데이터 가져오기");
                var bongBaseOptional =  bongBaseRepository.findByMinuteBong(minuteBong);
                if (bongBaseOptional.isEmpty()){
                    var errorMsg = "5-fail. " + minuteBong + "진입 데이터가 없습니다.";
                    log.error(errorMsg);
                    var slackNotificationLog =  slackNotificationLogRepository.save(
                            new SlackNotificationLog(
                                    null,
                                    slackNotification,
                                    targetSetRequestPath,
                                    targetSetMethodName,
                                    errorMsg,
                                    null,
                                    LocalDateTime.now()
                            )
                    );
                    SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);

                    return resultList;
                }

                log.info("6. 청산 기준 퍼센트 기준 확인");
                if (bongBaseOptional.get().getExitRates().size() == 0) {
                    // Slack 알람
                    log.error("6-fail. "+ minuteBong + "청산 퍼센트 기준 데이터가 없습니다.");
                    return resultList;
                }

                log.info("bongBase: 청산, 진입 기준 데이터");
                var bongBase = bongBaseOptional.get();

                log.info("7. [목표가 구하는 것]");
                log.info("Buy  = 현재가 + ( 현재가 - 기준(저점) )");
                log.info("Sell = 현재가 - ( 기준(고점) - 현재가 )");
                var targetPrice = isBuy ? price + (price - basePrice) : price - (basePrice - price);

                memberApis.forEach(

                        memberApi -> {

                            log.info("8. [Bybit REST] 나의 주문 리스트");
                            log.info("[Param Order Status]");
                            log.info("Created- 시스템에서 주문을 수락했지만 아직 매칭 엔진을 거치지 않은 경우");
                            log.info("New- 주문이 성공적으로 완료되었습니다.");
                            log.info("PartiallyFilled - 부분적 구매");
                            log.info("PendingCancel- 매칭 엔진이 취소 요청을 받았지만 성공적으로 취소되지 않았을 수 있습니다.");
                            var responseOrderList = BybitOrderUtil.order_list(
                                    memberApi.getApiKey(),
                                    memberApi.getSecretKey(),
                                    ORDER_STATUS.Created + ","
                                            + ORDER_STATUS.New + ","
                                            + ORDER_STATUS.PartiallyFilled + ","
                                            + ORDER_STATUS.PendingCancel
                            );

                            if (responseOrderList != null && responseOrderList.getStatusCode().equals(HttpStatus.OK) && responseOrderList.getBody() != null) {

                                try {

                                    log.info("9. [Bybit Rest] 나의 주문 리스트 조회");
                                    var bybitOrderList = objectMapper.readValue(responseOrderList.getBody().toString(), BybitMyOrder.class);

                                    if (bybitOrderList.getRet_code().equals(0)) {

                                        bybitOrderList.getResult().getData().forEach(
                                                data -> {

                                                    log.info("10. [Bybit Rest] 나의 주문 리스트를 가져온 뒤 주문이 안된 리스트는 취소");
                                                    var orderCancelResponse = BybitOrderUtil.order_cancel (
                                                            memberApi.getApiKey(),
                                                            memberApi.getSecretKey(),
                                                            data.getOrder_id()
                                                    );

                                                    if (orderCancelResponse == null || (!HttpStatus.OK.equals(orderCancelResponse.getStatusCode())) || orderCancelResponse.getBody() == null) {
                                                        // Slack 알림
                                                        log.error("10-fail. " + memberApi.getApiKey() + "," +  data.getOrder_id() + " 주문 취소 실패");
                                                    } else {

                                                        try {
                                                            var bybitMyOrderCancel = objectMapper.readValue(orderCancelResponse.getBody().toString(), BybitMyOrderCancel.class);
                                                            if (bybitMyOrderCancel.getRet_code() != 0) {
                                                                // Slack 알람
                                                                log.error("10-fail-2. "  + memberApi.getApiKey() + "," +  data.getOrder_id() + " 주문 취소 실패 (Bybit) Ret_code: " + bybitMyOrderCancel.getRet_code() + "(" +bybitMyOrderCancel.getRet_msg() + ")");
                                                            } else {

                                                                log.info("11. Bybit 주문 활성 취소를 성공시 주문[DB] 데이터도 삭제");
                                                                var traceEnterOptional = traceEnterRepository.findByOrderId(data.getOrder_id());
                                                                if (traceEnterOptional.isPresent()) {
                                                                    var traceEnter = traceEnterOptional.get();
                                                                    traceEnterRepository.delete(traceEnter);
                                                                }

                                                            }
                                                        } catch (JsonProcessingException e) {
                                                            // Slack 알림
                                                            log.error("10-fail-JsonProcessingException. "  + memberApi.getApiKey() + "," +  data.getOrder_id() + " 주문 취소 데이터 변환 (Bybit)");
                                                        }
                                                    }
                                                }
                                        );

                                        log.info("12. [Bybit Rest] 내가 가진 비트 코인 조회");
                                        var walletResponse = BybitOrderUtil.my_wallet(memberApi.getApiKey(), memberApi.getSecretKey());
                                        if (walletResponse == null || !walletResponse.getStatusCode().equals(HttpStatus.OK) || walletResponse.getBody() == null) {
                                            // Slack 알람
                                            log.error("12-fail. " + memberApi.getApiKey() + "내가 가진 비트 코인 조회를 실패했습니다. (bybit)");
                                        } else {
                                            try {
                                                var bybitMyWallet = objectMapper.readValue(walletResponse.getBody().toString(), BybitMyWallet.class);
                                                if (bybitMyWallet.getRet_code() == 0) {

                                                    log.info("13. [Bybit Rest] 포지션 조회 (Size)");
                                                    var positionQty = 0;
                                                    ResponseEntity<?> positionResponse = BybitOrderUtil.position(memberApi.getApiKey(), memberApi.getSecretKey());
                                                    if (positionResponse == null || !positionResponse.getStatusCode().equals(HttpStatus.OK) || positionResponse.getBody() == null) {
                                                        // Slack 알림
                                                        log.error("13-fail. " + memberApi.getApiKey() + ", 포지션 조회를 실패했습니다. (Bybit)");
                                                    } else {

                                                        try {
                                                            var bybitPosition =  objectMapper.readValue(positionResponse.getBody().toString(), BybitPosition.class);
                                                            if (bybitPosition.getRet_code() == 0) {
                                                                BybitPositionData bybitPositionData = bybitPosition.getResult();
                                                                positionQty = bybitPositionData.getSide().equals("Buy") ? bybitPositionData.getSize() : bybitPositionData.getSize() * -1;
                                                                log.info("position Side 여부에 따라 * -1 해줘야함 (" + positionQty + ")");
                                                            }
                                                        } catch (JsonProcessingException e ){
                                                            // Slack 알림
                                                            log.error("14-fail-JsonProcessingException. " + memberApi.getApiKey() + ", Bybit 포지션 조회 데이터 변환을 실패했습니다. (Bybit)");
                                                        }
                                                    }

                                                    log.info("15. [Bybit Rest] 다시 실제 거래내역 확인 [처음 조회했을때와 차익이 발생할 수도 있기 때문에 다시 확인을 해야함]");
                                                    var realPrice = 0.0;
                                                    var rePublicOrderResponseEntity = BybitOrderUtil.publicOrderList();
                                                    if (rePublicOrderResponseEntity.getBody() != null && rePublicOrderResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                                                        try {

                                                            var bybitRePublicOrder = objectMapper.readValue(rePublicOrderResponseEntity.getBody().toString(), BybitPublicOrder.class);
                                                            if (bybitRePublicOrder.getRet_code() != 0) {
                                                                // Slack 알람
                                                                log.error("15-fail-1. " + memberApi.getApiKey() + ", 다시 실제 거래내역 조회를 실패했습니다. (Bybit) Ret_code: " + bybitRePublicOrder.getRet_code() + "(" +bybitRePublicOrder.getRet_msg() + ")");
                                                            } else {

                                                                var bybitPublicOrderDataList = bybitRePublicOrder.getResult();

                                                                log.info("Side -> Buy Sell에 따라 최근 금액이 달라짐");
                                                                log.info("ex) Buy 일 경우 Buy 데이터로 비교");
                                                                for (var bybitPublicOrderData : bybitPublicOrderDataList) {
                                                                    if (isBuy && bybitPublicOrderData.getSide().equals(SIDE.Buy)) {
                                                                        realPrice = bybitPublicOrderData.getPrice();
                                                                        break;
                                                                    }else if (!isBuy && bybitPublicOrderData.getSide().equals(SIDE.Sell)) {
                                                                        realPrice = bybitPublicOrderData.getPrice();
                                                                        break;
                                                                    }
                                                                }

                                                                log.info("16. 내가 가진 비트 코인 ( Available_balance [Bybit 나의 지갑내역에서 확인] * 현재 가격)");
                                                                var myQty = bybitMyWallet.getResult().getBTC().getAvailable_balance() * realPrice;

                                                                log.info("17. 값이 실시간으로 변경되기 때문에 가격오르면 그 비율을 확인후 들어가도 되는지 안되는지 판단하기 위한 작업");
                                                                log.info("계산식 = 저점 + (목표가 - 저점) * 비율");
                                                                var enterPrice = basePrice + (targetPrice - basePrice) * bongBase.getEnterEndRate();
                                                                log.info("Buy: 기준금액 >= 실제금액, Sell: 기준금액 <= 실제금액 - 구매 가능");
                                                                if ( isBuy ? enterPrice >= realPrice : enterPrice <= realPrice  ) {

                                                                    log.info("18. 구매할 수 있는 수량");
                                                                    log.info("Buy  = 보유량 - (보유량 + 포지션)");
                                                                    log.info("Sell = 보유량 + (보유량 + 포지션)");
                                                                    var qty = (int) Math.floor(isBuy ? myQty - (myQty + positionQty) : myQty + (myQty + positionQty));

                                                                    log.info("19. [Bybit Rest] 구매 지정가" );
                                                                    var responseEntity = BybitOrderUtil.order(
                                                                            memberApi.getApiKey(),
                                                                            memberApi.getSecretKey(),
                                                                            qty,
                                                                            isBuy ? SIDE.Buy : SIDE.Sell,
                                                                            TIME_IN_FORCE.PostOnly,
                                                                            realPrice,
                                                                            ORDER_TYPE.Limit
                                                                    );

                                                                    if (responseEntity == null || !responseEntity.getStatusCode().equals(HttpStatus.OK) || responseEntity.getBody() == null) {
                                                                        // Slack 알림
                                                                        log.error("19-fail. 구매 지정가 조회 실패" + memberApi.getMember() + ", qty: " + (int) Math.floor(qty) + "realPrice: " + realPrice + " 구매를 실패했습니다. (Bybit)");
                                                                    } else {
                                                                        try {
                                                                            var bybitOrder = objectMapper.readValue(responseEntity.getBody().toString(), BybitOrder.class);
                                                                            if (bybitOrder.getRet_code() != 0) {
                                                                                // Slack 알림
                                                                                log.error("19-fail-2. 구매 지정가 조회 실패" + memberApi.getMember() + ", qty: "
                                                                                        + (int) Math.floor(qty) + "realPrice: " + realPrice + " 구매를 실패했습니다. (Bybit)  Ret_code: "
                                                                                        + bybitOrder.getRet_code() + "(" +bybitOrder.getRet_msg() + ")");
                                                                            } else {

                                                                                log.info("20. 거래 데이터가 있는지 확인");
                                                                                Optional<Trace> traceOptional = traceRepository.findByStartFlagAndMinuteBongAndMember_Idx(false, minuteBong, memberApi.getMember().getIdx());
                                                                                var trace = new Trace();
                                                                                if (traceOptional.isPresent()) {
                                                                                    log.info("21. 거래 데이터가 있을 경우");
                                                                                    trace = traceOptional.get();
                                                                                } else {
                                                                                    log.info("22. 거래 데이터가 없는 경우 - 거래 데이터 저장");
                                                                                    log.info("[판매가 내역 생성 안된 시점]");
                                                                                    log.info("손절 금액, 비율은 진입 종료 비율이 도달했을시 설정");
                                                                                    var traceParam = new Trace (
                                                                                        null,
                                                                                        memberApi.getMember(),
                                                                                        minuteBong,
                                                                                        basePrice,
                                                                                        price,
                                                                                        0.0,
                                                                                        bongBase.getEnterEndRate(),
                                                                                        0.0,
                                                                                        isBuy,
                                                                                        false,
                                                                                        false,
                                                                                        new ArrayList<>(),
                                                                                        new ArrayList<>(),
                                                                                        new ArrayList<>(),
                                                                                        LocalDateTime.now(),
                                                                                        LocalDateTime.now()
                                                                                    );
                                                                                    trace = traceRepository.save(traceParam);
                                                                                }

                                                                                log.info("23. 진입한 결과 상세 DB 저장");
                                                                                var traceEnterParam = new TraceEnter(bybitOrder.getResult());
                                                                                var traceEnter = traceEnterRepository.save(traceEnterParam);
                                                                                traceEnter.setTrace(trace);
                                                                                trace.getTraceEnters().add(traceEnter);


                                                                                log.info("24. 결과값 저장");
                                                                                resultList.add(trace);

                                                                            }
                                                                        }catch (JsonProcessingException e){
                                                                            // Slack 알림
                                                                            log.error("19-fail-JsonProcessingException. " + memberApi.getMember() + ", qty: " + (int) Math.floor(qty) + "realPrice: " + realPrice + " 구매를 실패했습니다. Json 변환 실패 (Bybit)");
                                                                        }
                                                                    }

                                                                } else {
                                                                    log.info("기준 지점을 넘어가면 거래안되게 설정" + ": enterPrice (" + enterPrice + "), realPrice (" + realPrice + ")");
                                                                }
                                                            }

                                                        } catch (JsonProcessingException e) {
                                                            // Slack 알람
                                                            log.error("15-fail-JsonProcessingException. " + memberApi.getApiKey() + ", 다시 실제 거래내역 조회를 실패했습니다. (Bybiy)");
                                                        }

                                                    } else {
                                                        // Slack 알람
                                                        log.error("15-fail-2. " + memberApi.getApiKey() + ", 다시 실제 거래내역 조회를 실패했습니다. (Bybit)");
                                                    }

                                                } else {
                                                    // Slack 알람
                                                    log.error("12-fail-2. " + memberApi.getApiKey() + "내가 가진 비트 코인 조회를 실패했습니다. (bybit) Ret_code: " + bybitMyWallet.getRet_code() + "(" +bybitMyWallet.getRet_msg() + ")");
                                                }

                                            } catch (JsonProcessingException e) {
                                                // Slack 알람
                                                log.error("12-fail-JsonProcessingException. " + memberApi.getApiKey() + "내가 가진 비트 코인 데이터 변환을 실패했습니다. (bybit)");
                                            }
                                        }

                                    } else {
                                        log.error("9-fail. " + memberApi.member.getApis() + " 나의 주문 리스트 조회를 실패했습니다. Ret_code: " + bybitOrderList.getRet_code() + "(" +bybitOrderList.getRet_msg() + ")");
                                    }

                                } catch (JsonProcessingException e) {
                                    // Slack 알림
                                    log.error("9-fail-JsonProcessingException. memberApi.member.getApis() + \" 나의 주문 리스트 데이터 변환을 실패했습니다. (ByBit)");
                                }
                            } else {
                                // Slack 알람
                                log.info("8-fail. " + memberApi.member.getApis() + " 나의 주문 리스트 조회를 실패했습니다. (ByBit)");
                            }
                        }
                );
            }
        }
        return resultList;
    }

    /**
     * 진입 금액이 맞는지 확인 후 청산 금액 세팅
     * @param minuteBong 봉
     * @return List<Trace>
     */
    @Transactional
    public List<Trace> traceExitSet(Integer minuteBong) {

        var objectMapper = new ObjectMapper();

        log.info("1. resultList: 결과값 Return 해주기 위한 데이터");
        var resultList = new ArrayList<Trace>();

        log.info("2. traces: DB 상으로 거래가 시작 안된 리스트 조회");
        var traces = traceRepository.findByStartFlagAndMinuteBong(false, minuteBong);

        log.info("3. 봉 수익,손절 퍼센트 기준, 구매 중지 데이터 가져오기");
        var bongBaseOptional = bongBaseRepository.findByMinuteBong(minuteBong);
        if (bongBaseOptional.isEmpty()){
            // Slack 알람
            log.error("3-fail. " + minuteBong + "진입 데이터가 없습니다.");
            return resultList;
        }

        log.info("3-1. 청산 기준 퍼센트 기준 확인");
        if (bongBaseOptional.get().getExitRates().size() == 0) {
            // Slack 알람
            log.error("3-1-fail. "+ minuteBong + "청산 퍼센트 기준 데이터가 없습니다.");
            return resultList;
        }

        log.info("bongBase: 청산, 진입 기준 데이터");
        var bongBase = bongBaseOptional.get();


        traces.forEach(
                trace -> {

                    log.info("4. 순차적으로 판매 비율을 계산하기 위한 정렬 (Sort 기준) - [판매금액 설정]");
                    var exitRates = bongBase
                            .getExitRates()
                            .stream()
                            .sorted(Comparator.comparing(BongBaseExitRate::getSort))
                            .collect(Collectors.toList());

                    var isBuy   = trace.isBuyFlag();
                    var member  = trace.getMember();

                    log.info("5. 거래된 내역의 Api key를 확인하기 위해 조회");
                    var memberApiOptional = memberApiRepository.findByMinuteBongAndMemberIdx(minuteBong, member.getIdx());

                    if (memberApiOptional.isPresent()) {

                        var memberApi = memberApiOptional.get();

                        log.info("6. [Bybit Rest] 나의 주문 리스트");
                        log.info("실질적으로 거래가 되었는지 확인해야함 - 확인이 된 경우에만 거래해야하고 아니다면 데이터 삭제해야함");
                        log.info("Filled - 주문이 다 팔린 것");
                        log.info("PartiallyFilled - 부분적 구매");
                        var responseOrderList = BybitOrderUtil.order_list(
                                memberApi.getApiKey(),
                                memberApi.getSecretKey(),
                                ORDER_STATUS.Filled + "," + ORDER_STATUS.PartiallyFilled
                        );

                        if (responseOrderList != null && responseOrderList.getStatusCode().equals(HttpStatus.OK) && responseOrderList.getBody() != null) {

                            try {
                                var bybitMyOrder = objectMapper.readValue(responseOrderList.getBody().toString(), BybitMyOrder.class);
                                if (!bybitMyOrder.getRet_code().equals(0)) {
                                    // Slack 알림
                                    log.error("6-fail. [Bybit Rest] 나의 주문 리스트 조회 실패 Ret_code: " + bybitMyOrder.getRet_code() + "(" +bybitMyOrder.getRet_msg() + ")");
                                } else {

                                    var bybitMyOrderResult = bybitMyOrder.getResult();
                                    var myOrderDataList = bybitMyOrderResult.getData();

                                    if (myOrderDataList == null || myOrderDataList.size() == 0) {
                                        // Slack 알림
                                        log.error("6-fail-3. [Bybit Rest] 조회된 내역 리스트가 없습니다.");
                                    } else {
                                        log.info("7. 비트코인 구매 내역 조회");
                                        var traceEnterList = trace.getTraceEnters();
                                        if(traceEnterList.size() == 0) {
                                            // Slack
                                            log.error("7-fail. " + trace.getIdx() + " 구매 내역 데이터가 올바르지 않습니다. Trace Idx(" + trace.getIdx() + ") 진입한 데이터가 없습니다.");
                                        } else {



                                            log.info("9. Bybit 주문된 리스트와 DB상 리스트와 비교");
                                            log.info("myOrderDataListFilled: 완전 구매 내역의 리스트");
                                            var myOrderDataListFilled
                                                    = myOrderDataList.stream()
                                                        .filter(data -> data.getOrder_status().equals(ORDER_STATUS.Filled))
                                                        .collect(Collectors.toList());


                                            log.info("myOrderDataListPartiallyFilled: 부분 구매 완료내역의 리스트");
                                            var myOrderDataListPartiallyFilled
                                                    = myOrderDataList.stream()
                                                    .filter(data -> data.getOrder_status().equals(ORDER_STATUS.PartiallyFilled))
                                                    .collect(Collectors.toList());

                                            var deleteTraceEnter = new ArrayList<TraceEnter>();
                                            traceEnterList.forEach(
                                                    traceEnter -> {

                                                        if (!myOrderDataListFilled.stream()
                                                                .map(BybitMyOrderData::getOrder_id)
                                                                .collect(Collectors.toList())
                                                                .contains(traceEnter.getOrderId())) {

                                                            log.info("정상 구매가 되지않았을 경우");

                                                            // TODO 일부만 구매 되었을 경우 [금액 조정, qty 조정]
                                                            log.info("부분 구매가 된 경우");
                                                            if (myOrderDataListPartiallyFilled.stream()
                                                                    .map(BybitMyOrderData::getOrder_id)
                                                                    .collect(Collectors.toList())
                                                                    .contains(traceEnter.getOrderId())) {

                                                                // TODO 작업
                                                                log.info("취소 처리하고 수량 조절");

                                                            } else {
                                                                log.info("부분 구매도 안된 데이터는 삭제");
                                                                deleteTraceEnter.add(traceEnter);
                                                            }



                                                        } else {

                                                            log.info("정상 구매가 완료되었을 때");
                                                            log.info("DB <-> Bybit 데이터가 맞는지 확인 후 일치화 시켜야함");
                                                            // TODO 작업
                                                        }
                                                    }
                                            );

                                            log.info("부분 구매도 안된 데이터는 삭제 (DB)");
                                            traceEnterRepository.deleteAll(deleteTraceEnter);

                                            // TODO 작업
                                            log.info("8. 거래 데이터에 봉 수익, 손절 퍼센트 기준, 구매 중지 데이터 저장");

                                            // TODO Trace 값 수정 (lossPrice, enterEndRate, lossRate)

                                            log.info("qty: 비율 계산 해야함(올림)");
                                            log.info("addQty: 누적으로 계산한뒤 총 qty 에서 마지막 전 데이터까지 누적 qty 를 빼주기 위해 계산");
                                            int totalQty = traceEnterList.stream().map(TraceEnter::getQty).reduce(0, Integer::sum);
                                            var addQty = 0;

                                            for ( var exitRate : exitRates ) {

                                                log.info("청산 exitQty");
                                                var exitQty = 0;

                                                if (exitRate.getSort() == exitRates.size()) {
                                                    exitQty = totalQty - addQty;
                                                    log.info("exitQty Last: " + exitQty);
                                                } else {
                                                    var traceRate = exitRate.getTraceRate();
                                                    exitQty = (int) Math.ceil(exitQty * ((double) traceRate / 100));
                                                    addQty += exitQty;
                                                    log.info("exitQty (" + exitRate.getSort() + "): " + exitQty + "(" + addQty + ")");
                                                }

                                                log.info("[목표가 계산하기]");
                                                log.info("Buy  = 현재가 + ( 현재가 - 기준(저점) )");
                                                log.info("Sell = 현재가 - ( 기준(고점) - 현재가 )");
                                                var price       = trace.getPrice();     /* 현재가 (진입금액) */
                                                var basePrice   = trace.getBasePrice(); /* 기준(저점, 고점) */
                                                var targetPrice = isBuy ? price + (price - basePrice) : price - (basePrice - price); /* 목표가 */


                                                log.info("[청산 금액, 손절 금액 계산하기]");
                                                log.info("계산식 = 저점 + (목표가 - 저점) * 비율");
                                                var exitPrice = basePrice + (targetPrice - basePrice) * exitRate.getExitRate();
                                                var lossPrice = basePrice + (targetPrice - basePrice) * exitRate.getLossRate();

                                                log.info("[Bybit Rest] 청산 지정가" );
                                                log.info("Buy -> Sell, Sell -> Buy 반대로 지정해야함");
                                                var responseEntity = BybitOrderUtil.order(
                                                        memberApi.getApiKey(),
                                                        memberApi.getSecretKey(),
                                                        exitQty,
                                                        isBuy ? SIDE.Sell : SIDE.Buy,
                                                        TIME_IN_FORCE.PostOnly,
                                                        exitPrice,
                                                        ORDER_TYPE.Limit
                                                );

                                                if (responseEntity == null || !responseEntity.getStatusCode().equals(HttpStatus.OK) || responseEntity.getBody() == null) {
                                                    // Slack 알림
                                                    log.error("청산 지정가 조회 실패" + memberApi.getMember() + ", exitQty: " + exitQty + "exitPrice: " + exitPrice + " 청산를 실패했습니다. (Bybit)");
                                                } else {

                                                    try {
                                                        var bybitOrder = objectMapper.readValue(responseEntity.getBody().toString(), BybitOrder.class);
                                                        if (bybitOrder.getRet_code() != 0) {
                                                            // Slack 알림
                                                            log.error("청산 지정가 실패" + memberApi.getMember() + ", exitQty: "
                                                                    + exitQty + "exitPrice: " + exitPrice + " 청산을 실패했습니다. (Bybit)  Ret_code: "
                                                                    + bybitOrder.getRet_code() + "(" +bybitOrder.getRet_msg() + ")");

                                                        } else {

                                                            log.info("정상 처리되었을시 저장");
                                                            var traceBaseListParam = new TraceExit(bybitOrder.getResult(), exitRate.getSort());
                                                            var traceBaseList = traceExitRepository.save(traceBaseListParam);

                                                            log.info("손절 금액 설정");
                                                            traceBaseList.setStopLossPrice(lossPrice);
                                                            traceBaseList.setTrace(trace);

                                                            trace.getTraceExits().add(traceBaseList);
                                                        }

                                                    }catch (JsonProcessingException e){
                                                        // Slack 알림
                                                        log.error("" + memberApi.getMember() + ", exitQty: " + exitQty + "exitPrice: " + exitPrice + " 청산을 실패했습니다. Json 변환 실패 (Bybit)");
                                                    }
                                                }
                                            }

                                            log.info("청산 처리가 완료된 경우 저장 결과값 리턴 저장");
                                            trace.setStartFlag(true);
                                            resultList.add(trace);
                                        }
                                    }
                                }
                            } catch (JsonProcessingException e) {
                                // Slack 알림
                                log.error("6-fail-JsonProcessingException. [Bybit Rest] 나의 주문 리스트 데이터 변환 실패: " + memberApi.getApiKey());
                            }
                        } else {
                            // Slack 알람
                            log.error("6-fail-2. [Bybit Rest] 나의 주문 리스트 조회 실패");
                        }

                    } else {
                        // Slack 알림
                        log.error("5-fail. 회원의 Api Key 를 찾을 수 없습니다." + member.getIdx());
                    }
                }
        );

        return resultList;
    }
}
