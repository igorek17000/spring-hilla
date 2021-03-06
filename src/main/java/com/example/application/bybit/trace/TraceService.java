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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class TraceService {

    private final TraceRepository traceRepository;
    private final TraceExitRepository traceExitRepository;
    private final TraceEnterRepository traceEnterRepository;
    private final TraceExitRateRepository traceExitRateRepository;
    private final MemberRepository memberRepository;
    private final MemberApiRepository memberApiRepository;
    private final BongBaseRepository bongBaseRepository;
    private final BongRepository bongRepository;
    private final SlackNotificationRepository slackNotificationRepository;
    private final SlackNotificationLogRepository slackNotificationLogRepository;


    private final String targetSetRequestPath = "/trace/target/set";
    private final String exitSetRequestPath   = "/trace/exit/set";
    private final String checkRequestPath  = "/trace/check";
    private final String exitSetMethodName    = "traceExitSet";
    private final String targetSetMethodName  = "traceTargetSet";
    private final String checkMethodName    = "check";

    /**
     * ?????? ?????? ????????? ??? ??????
     * @param minuteBong ???
     */
    public CheckResult check (Integer minuteBong) {
        var result = new CheckResult();

        log.info("Slack ?????? ????????? ?????? ??????");
        var slackNotificationOptional = slackNotificationRepository.findById(1);
        if (slackNotificationOptional.isEmpty()){
            result.setResult(CHECK_RESULT.NO_SLACK);
            return result;
        }
        var slackNotification = slackNotificationOptional.get();

        log.info("1. ?????? ?????? ?????? ?????? ????????? ??????");
        var traceEnterSetList
                = traceRepository.findByStartFlagAndEndFlagAndMinuteBong(false, false, minuteBong);
        if (traceEnterSetList.size() > 0) {
            result.setResult(CHECK_RESULT.ENTER_SET);
            return result;
        }



        var traceSetCompletionList
                = traceRepository.findByStartFlagAndEndFlagAndMinuteBong(true, false, minuteBong);

        log.info("2. ?????? ?????? ?????? ?????? ????????? ??????");
        log.info("?????? ???????????? ?????? ?????? ?????? ???????????? ?????? ????????? ??????, ?????? ???????????? ?????? ??????");
        log.info("[????????? ????????? ?????? ???????????? true ??? ?????? ?????? ????????? ???????????? ??????]");
        for (var trace : traceSetCompletionList) {
            var exitListSize = trace.getTraceExits().size();

            if (exitListSize == 0 || (trace.getTraceExits().size() != trace.getTraceExitsRates().size())) {
                var errorMsg = "2-fail. IDX: " + trace.getIdx() + " ?????? ????????? ????????? ???????????????.";
                log.error(errorMsg);
                var slackNotificationLog =  slackNotificationLogRepository.save(
                        new SlackNotificationLog(
                                null,
                                slackNotification,
                                checkRequestPath,
                                checkMethodName,
                                errorMsg,
                                null,
                                LocalDateTime.now()
                        )
                );
                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                result.setResult(CHECK_RESULT.EXIT_SET_ERROR);
                return result;
            }
        }

        log.info("3. ??????, ?????? ?????? ??? ?????? ?????? ?????? ????????? ??????");
        if (traceSetCompletionList.size() > 0) {
            result.setResult(CHECK_RESULT.SET_COMPLETION);
            return result;
        }

        log.info("4. ??? ????????? ?????? ????????? ??????");
        var bongList = bongRepository.findByMinuteBong(minuteBong);
        if (bongList.size() == 0) {
            var errorMsg = "4-fail. ??? ????????? ???????????? ????????????.";
            log.error(errorMsg);
            var slackNotificationLog =  slackNotificationLogRepository.save(
                    new SlackNotificationLog(
                            null,
                            slackNotification,
                            checkRequestPath,
                            checkMethodName,
                            errorMsg,
                            null,
                            LocalDateTime.now()
                    )
            );
            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
            result.setResult(CHECK_RESULT.NO_BONG);
            return result;
        }

        log.info("5. ??? ????????? ?????? ??????");
        result.getBongList().addAll(bongList);
        return result;
    }

    /**
     * ?????? ?????? ??????
     * @param minuteBong ???
     * @param price ?????? ?????? (?????? ????????? ??????)
     * @param isBuy ??????,?????? ??????
     * @param basePrice ??????, ??????
     * @return List<Trace>
     */
    @Transactional
    public TraceTargetSetResult traceTargetSet(
            Integer minuteBong,
            Double price,
            boolean isBuy,
            Double basePrice
    ) {

        log.info("1. result: ????????? Return ????????? ?????? ??????, resultTraceList: ????????? ??? ?????? Return ????????? ?????? ??????");
        var result = new TraceTargetSetResult();
        var resultTraceList = new ArrayList<Trace>();

        log.info("Slack ?????? ????????? ?????? ??????");
        var objectMapper = new ObjectMapper();
        var slackNotificationOptional = slackNotificationRepository.findById(1);
        if (slackNotificationOptional.isEmpty()){
            result.setResult(TARGET_RESULT.NO_SLACK);
            return result;
        }
        var slackNotification = slackNotificationOptional.get();

        log.info("2. members: ???????????? ???????????? ?????? ????????? ?????? ????????????");
        var members = memberRepository.findByTraceYn("Y");
        if (members.size() > 0 ) {

            log.info("3. memberApis: Api Key, Secret Key ?????? (??????????????? ????????? ?????? ApiKey ????????????)");
            log.info("4. memberApis Param: ???????????? ???????????? ???????????? ??????");
            var memberApis
                    = memberApiRepository.findByMinuteBongAndMemberIdxIn (
                            minuteBong,
                            // 3-1
                            members.stream().map(Member::getIdx).collect(Collectors.toList())
                    );

            if (memberApis.size() > 0) {

                log.info("5. ?????? ?????? ????????? ????????????");
                var bongBaseOptional =  bongBaseRepository.findByMinuteBong(minuteBong);
                if (bongBaseOptional.isEmpty()){
                    var errorMsg = "5-fail. " + minuteBong + "?????? ???????????? ????????????.";
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

                    result.setResult(TARGET_RESULT.NO_BONG_TARGET_BASE);
                    return result;
                }

                log.info("6. ?????? ?????? ????????? ?????? ??????");
                if (bongBaseOptional.get().getExitRates().size() == 0) {

                    var errorMsg = "6-fail. "+ minuteBong + "?????? ????????? ?????? ???????????? ????????????.";
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

                    result.setResult(TARGET_RESULT.NO_BONG_EXIT_BASE);
                    return result;
                }

                log.info("bongBase: ??????, ?????? ?????? ?????????");
                var bongBase = bongBaseOptional.get();

                log.info("7. [????????? ????????? ???]");
                log.info("Buy  = ????????? + ( ????????? - ??????(??????) )");
                log.info("Sell = ????????? - ( ??????(??????) - ????????? )");
                var targetPrice = isBuy ? price + (price - basePrice) : price - (basePrice - price);

                memberApis.forEach(

                        memberApi -> {

                            log.info("8. [Bybit REST] ?????? ?????? ?????????");
                            log.info("[Param Order Status]");
                            log.info("Created- ??????????????? ????????? ??????????????? ?????? ?????? ????????? ????????? ?????? ??????");
                            log.info("New- ????????? ??????????????? ?????????????????????.");
                            log.info("PartiallyFilled - ????????? ??????");
                            log.info("PendingCancel- ?????? ????????? ?????? ????????? ???????????? ??????????????? ???????????? ????????? ??? ????????????.");
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

                                    log.info("9. [Bybit Rest] ?????? ?????? ????????? ??????");
                                    var bybitOrderList = objectMapper.readValue(responseOrderList.getBody().toString(), BybitMyOrder.class);

                                    if (bybitOrderList.getRet_code().equals(0)) {

                                        bybitOrderList.getResult().getData().forEach(
                                                data -> {

                                                    log.info("10. [Bybit Rest] ?????? ?????? ???????????? ????????? ??? ????????? ?????? ???????????? ??????");
                                                    var orderCancelResponse = BybitOrderUtil.order_cancel (
                                                            memberApi.getApiKey(),
                                                            memberApi.getSecretKey(),
                                                            data.getOrder_id()
                                                    );

                                                    if (orderCancelResponse == null || (!HttpStatus.OK.equals(orderCancelResponse.getStatusCode())) || orderCancelResponse.getBody() == null) {
                                                        var errorMsg = "10-fail. " + memberApi.getApiKey() + "," +  data.getOrder_id() + " ?????? ?????? ??????";
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
                                                    } else {

                                                        try {
                                                            var bybitMyOrderCancel = objectMapper.readValue(orderCancelResponse.getBody().toString(), BybitMyOrderCancel.class);
                                                            if (bybitMyOrderCancel.getRet_code() != 0) {
                                                                var errorMsg = "10-fail-2. "  + memberApi.getApiKey() + "," +  data.getOrder_id() + " ?????? ?????? ?????? (Bybit) Ret_code: " + bybitMyOrderCancel.getRet_code() + "(" +bybitMyOrderCancel.getRet_msg() + ")";
                                                                log.error(errorMsg);
                                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                        new SlackNotificationLog(
                                                                                null,
                                                                                slackNotification,
                                                                                targetSetRequestPath,
                                                                                targetSetMethodName,
                                                                                errorMsg,
                                                                                "",
                                                                                LocalDateTime.now()
                                                                        )
                                                                );
                                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                            } else {

                                                                log.info("11. Bybit ?????? ?????? ????????? ????????? ??????[DB] ???????????? ??????");
                                                                var traceEnterOptional = traceEnterRepository.findByOrderId(data.getOrder_id());
                                                                if (traceEnterOptional.isPresent()) {
                                                                    var traceEnter = traceEnterOptional.get();
                                                                    traceEnterRepository.delete(traceEnter);
                                                                }

                                                            }
                                                        } catch (JsonProcessingException e) {
                                                            var stringWriter = new StringWriter();
                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                            var errorMsg = "10-fail-JsonProcessingException. "  + memberApi.getApiKey() + "," +  data.getOrder_id() + " ?????? ?????? ????????? ?????? (Bybit)";
                                                            log.error(errorMsg);
                                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                    new SlackNotificationLog(
                                                                            null,
                                                                            slackNotification,
                                                                            targetSetRequestPath,
                                                                            targetSetMethodName,
                                                                            errorMsg,
                                                                            stringWriter.toString(),
                                                                            LocalDateTime.now()
                                                                    )
                                                            );
                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                        }
                                                    }
                                                }
                                        );

                                        log.info("12. [Bybit Rest] ?????? ?????? ?????? ?????? ??????");
                                        var walletResponse = BybitOrderUtil.my_wallet(memberApi.getApiKey(), memberApi.getSecretKey());
                                        if (walletResponse == null || !walletResponse.getStatusCode().equals(HttpStatus.OK) || walletResponse.getBody() == null) {
                                            var errorMsg = "12-fail. " + memberApi.getApiKey() + "?????? ?????? ?????? ?????? ????????? ??????????????????. (bybit)";
                                            log.error(errorMsg);
                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                    new SlackNotificationLog(
                                                            null,
                                                            slackNotification,
                                                            targetSetRequestPath,
                                                            targetSetMethodName,
                                                            errorMsg,
                                                            "",
                                                            LocalDateTime.now()
                                                    )
                                            );
                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                        } else {
                                            try {
                                                var bybitMyWallet = objectMapper.readValue(walletResponse.getBody().toString(), BybitMyWallet.class);
                                                if (bybitMyWallet.getRet_code() == 0) {

                                                    log.info("13. [Bybit Rest] ????????? ?????? (Size)");
                                                    var positionQty = 0;
                                                    ResponseEntity<?> positionResponse = BybitOrderUtil.position(memberApi.getApiKey(), memberApi.getSecretKey());
                                                    if (positionResponse == null || !positionResponse.getStatusCode().equals(HttpStatus.OK) || positionResponse.getBody() == null) {
                                                        var errorMsg = "13-fail. " + memberApi.getApiKey() + ", ????????? ????????? ??????????????????. (Bybit)";
                                                        log.error(errorMsg);
                                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                new SlackNotificationLog(
                                                                        null,
                                                                        slackNotification,
                                                                        targetSetRequestPath,
                                                                        targetSetMethodName,
                                                                        errorMsg,
                                                                        "",
                                                                        LocalDateTime.now()
                                                                )
                                                        );
                                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                    } else {

                                                        try {
                                                            var bybitPosition =  objectMapper.readValue(positionResponse.getBody().toString(), BybitPosition.class);
                                                            if (bybitPosition.getRet_code() == 0) {
                                                                BybitPositionData bybitPositionData = bybitPosition.getResult();
                                                                positionQty = bybitPositionData.getSide().equals("Buy") ? bybitPositionData.getSize() : bybitPositionData.getSize() * -1;
                                                                log.info("position Side ????????? ?????? * -1 ???????????? (" + positionQty + ")");
                                                            }
                                                        } catch (JsonProcessingException e ){
                                                            var stringWriter = new StringWriter();
                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                            var errorMsg = "14-fail-JsonProcessingException. " + memberApi.getApiKey() + ", Bybit ????????? ?????? ????????? ????????? ??????????????????. (Bybit)";
                                                            log.error(errorMsg);
                                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                    new SlackNotificationLog(
                                                                            null,
                                                                            slackNotification,
                                                                            targetSetRequestPath,
                                                                            targetSetMethodName,
                                                                            errorMsg,
                                                                            stringWriter.toString(),
                                                                            LocalDateTime.now()
                                                                    )
                                                            );
                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                        }
                                                    }

                                                    log.info("15. [Bybit Rest] ?????? ?????? ???????????? ?????? [?????? ?????????????????? ????????? ????????? ?????? ?????? ????????? ?????? ????????? ?????????]");
                                                    var realPrice = 0.0;
                                                    var rePublicOrderResponseEntity = BybitOrderUtil.publicOrderList();
                                                    if (rePublicOrderResponseEntity.getBody() != null && rePublicOrderResponseEntity.getStatusCode().equals(HttpStatus.OK)) {

                                                        try {

                                                            var bybitRePublicOrder = objectMapper.readValue(rePublicOrderResponseEntity.getBody().toString(), BybitPublicOrder.class);
                                                            if (bybitRePublicOrder.getRet_code() != 0) {
                                                                var errorMsg = "15-fail-1. " + memberApi.getApiKey() + ", ?????? ?????? ???????????? ????????? ??????????????????. (Bybit) Ret_code: " + bybitRePublicOrder.getRet_code() + "(" +bybitRePublicOrder.getRet_msg() + ")";
                                                                log.error(errorMsg);
                                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                        new SlackNotificationLog(
                                                                                null,
                                                                                slackNotification,
                                                                                targetSetRequestPath,
                                                                                targetSetMethodName,
                                                                                errorMsg,
                                                                                "",
                                                                                LocalDateTime.now()
                                                                        )
                                                                );
                                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                            } else {

                                                                var bybitPublicOrderDataList = bybitRePublicOrder.getResult();

                                                                log.info("Side -> Buy Sell??? ?????? ?????? ????????? ?????????");
                                                                log.info("ex) Buy ??? ?????? Buy ???????????? ??????");
                                                                for (var bybitPublicOrderData : bybitPublicOrderDataList) {
                                                                    if (isBuy && bybitPublicOrderData.getSide().equals(SIDE.Buy)) {
                                                                        realPrice = bybitPublicOrderData.getPrice();
                                                                        break;
                                                                    }else if (!isBuy && bybitPublicOrderData.getSide().equals(SIDE.Sell)) {
                                                                        realPrice = bybitPublicOrderData.getPrice();
                                                                        break;
                                                                    }
                                                                }

                                                                log.info("16. ?????? ?????? ?????? ?????? ( Available_balance [Bybit ?????? ?????????????????? ??????] * ?????? ??????)");
                                                                var myQty = bybitMyWallet.getResult().getBTC().getAvailable_balance() * realPrice;

                                                                log.info("17. ?????? ??????????????? ???????????? ????????? ??????????????? ??? ????????? ????????? ???????????? ????????? ???????????? ???????????? ?????? ??????");
                                                                log.info("????????? = ?????? + (????????? - ??????) * ??????");
                                                                var enterPrice = basePrice + (targetPrice - basePrice) * bongBase.getEnterEndRate();
                                                                log.info("Buy: ???????????? >= ????????????, Sell: ???????????? <= ???????????? - ?????? ??????");
                                                                if ( isBuy ? enterPrice >= realPrice : enterPrice <= realPrice  ) {

                                                                    log.info("18. ????????? ??? ?????? ??????");
                                                                    log.info("Buy  = ????????? - (????????? + ?????????)");
                                                                    log.info("Sell = ????????? + (????????? + ?????????)");
                                                                    var qty = (int) Math.floor(isBuy ? myQty - (myQty + positionQty) : myQty + (myQty + positionQty));

                                                                    log.info("19. [Bybit Rest] ?????? ?????????" );
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
                                                                        var errorMsg = "19-fail. ?????? ????????? ?????? ??????" + memberApi.getMember() + ", qty: " + (int) Math.floor(qty) + "realPrice: " + realPrice + " ????????? ??????????????????. (Bybit)";
                                                                        log.error(errorMsg);
                                                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                                new SlackNotificationLog(
                                                                                        null,
                                                                                        slackNotification,
                                                                                        targetSetRequestPath,
                                                                                        targetSetMethodName,
                                                                                        errorMsg,
                                                                                        "",
                                                                                        LocalDateTime.now()
                                                                                )
                                                                        );
                                                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                    } else {
                                                                        try {
                                                                            var bybitOrder = objectMapper.readValue(responseEntity.getBody().toString(), BybitOrder.class);
                                                                            if (bybitOrder.getRet_code() != 0) {
                                                                                var errorMsg = "19-fail-2. ?????? ????????? ?????? ??????" + memberApi.getMember() + ", qty: "
                                                                                        + (int) Math.floor(qty) + "realPrice: " + realPrice + " ????????? ??????????????????. (Bybit)  Ret_code: "
                                                                                        + bybitOrder.getRet_code() + "(" +bybitOrder.getRet_msg() + ")";
                                                                                log.error(errorMsg);
                                                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                                        new SlackNotificationLog(
                                                                                                null,
                                                                                                slackNotification,
                                                                                                targetSetRequestPath,
                                                                                                targetSetMethodName,
                                                                                                errorMsg,
                                                                                                "",
                                                                                                LocalDateTime.now()
                                                                                        )
                                                                                );
                                                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                            } else {

                                                                                log.info("20. ?????? ???????????? ????????? ??????");
                                                                                Optional<Trace> traceOptional = traceRepository.findByStartFlagAndMinuteBongAndMember_Idx(false, minuteBong, memberApi.getMember().getIdx());
                                                                                var trace = new Trace();
                                                                                if (traceOptional.isPresent()) {
                                                                                    log.info("21. ?????? ???????????? ?????? ??????");
                                                                                    trace = traceOptional.get();
                                                                                } else {
                                                                                    log.info("22. ?????? ???????????? ?????? ?????? - ?????? ????????? ??????");
                                                                                    log.info("[????????? ?????? ?????? ?????? ??????]");
                                                                                    log.info("?????? ??????, ????????? ?????? ?????? ????????? ??????????????? ??????");
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

                                                                                log.info("23. ????????? ?????? ?????? DB ??????");
                                                                                var traceEnterParam = new TraceEnter(bybitOrder.getResult());
                                                                                var traceEnter = traceEnterRepository.save(traceEnterParam);
                                                                                traceEnter.setTrace(trace);
                                                                                trace.getTraceEnters().add(traceEnter);


                                                                                log.info("24. ????????? ??????");
                                                                                resultTraceList.add(trace);

                                                                            }
                                                                        }catch (JsonProcessingException e){
                                                                            var stringWriter = new StringWriter();
                                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                                            var errorMsg = "19-fail-JsonProcessingException. " + memberApi.getMember() + ", qty: " + (int) Math.floor(qty) + "realPrice: " + realPrice + " ????????? ??????????????????. Json ?????? ?????? (Bybit)";
                                                                            log.error(errorMsg);
                                                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                                    new SlackNotificationLog(
                                                                                            null,
                                                                                            slackNotification,
                                                                                            targetSetRequestPath,
                                                                                            targetSetMethodName,
                                                                                            errorMsg,
                                                                                            stringWriter.toString(),
                                                                                            LocalDateTime.now()
                                                                                    )
                                                                            );
                                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                        }
                                                                    }

                                                                } else {
                                                                    log.info("?????? ????????? ???????????? ??????????????? ??????" + ": enterPrice (" + enterPrice + "), realPrice (" + realPrice + ")");
                                                                }
                                                            }

                                                        } catch (JsonProcessingException e) {
                                                            var stringWriter = new StringWriter();
                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                            var errorMsg = "15-fail-JsonProcessingException. " + memberApi.getApiKey() + ", ?????? ?????? ???????????? ????????? ??????????????????. (Bybiy)";
                                                            log.error(errorMsg);
                                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                    new SlackNotificationLog(
                                                                            null,
                                                                            slackNotification,
                                                                            targetSetRequestPath,
                                                                            targetSetMethodName,
                                                                            errorMsg,
                                                                            stringWriter.toString(),
                                                                            LocalDateTime.now()
                                                                    )
                                                            );
                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                        }

                                                    } else {
                                                        var errorMsg = "15-fail-2. " + memberApi.getApiKey() + ", ?????? ?????? ???????????? ????????? ??????????????????. (Bybit)";
                                                        log.error(errorMsg);
                                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                new SlackNotificationLog(
                                                                        null,
                                                                        slackNotification,
                                                                        targetSetRequestPath,
                                                                        targetSetMethodName,
                                                                        errorMsg,
                                                                        "",
                                                                        LocalDateTime.now()
                                                                )
                                                        );
                                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                    }
                                                } else {
                                                    var errorMsg = "12-fail-2. " + memberApi.getApiKey() + "?????? ?????? ?????? ?????? ????????? ??????????????????. (bybit) Ret_code: " + bybitMyWallet.getRet_code() + "(" +bybitMyWallet.getRet_msg() + ")";
                                                    log.error(errorMsg);
                                                    var slackNotificationLog =  slackNotificationLogRepository.save(
                                                            new SlackNotificationLog(
                                                                    null,
                                                                    slackNotification,
                                                                    targetSetRequestPath,
                                                                    targetSetMethodName,
                                                                    errorMsg,
                                                                    "",
                                                                    LocalDateTime.now()
                                                            )
                                                    );
                                                    SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                }

                                            } catch (JsonProcessingException e) {
                                                var stringWriter = new StringWriter();
                                                e.printStackTrace(new PrintWriter(stringWriter));
                                                var errorMsg = "12-fail-JsonProcessingException. " + memberApi.getApiKey() + "?????? ?????? ?????? ?????? ????????? ????????? ??????????????????. (bybit)";
                                                log.error(errorMsg);
                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                        new SlackNotificationLog(
                                                                null,
                                                                slackNotification,
                                                                targetSetRequestPath,
                                                                targetSetMethodName,
                                                                errorMsg,
                                                                stringWriter.toString(),
                                                                LocalDateTime.now()
                                                        )
                                                );
                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                            }
                                        }

                                    } else {
                                        var errorMsg = "9-fail. " + memberApi.member.getApis() + " ?????? ?????? ????????? ????????? ??????????????????. Ret_code: " + bybitOrderList.getRet_code() + "(" +bybitOrderList.getRet_msg() + ")";
                                        log.error(errorMsg);
                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                new SlackNotificationLog(
                                                        null,
                                                        slackNotification,
                                                        targetSetRequestPath,
                                                        targetSetMethodName,
                                                        errorMsg,
                                                        "",
                                                        LocalDateTime.now()
                                                )
                                        );
                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                    }
                                } catch (JsonProcessingException e) {
                                    var stringWriter = new StringWriter();
                                    e.printStackTrace(new PrintWriter(stringWriter));
                                    var errorMsg = "9-fail-JsonProcessingException. "+ memberApi.member.getApis() + " ?????? ?????? ????????? ????????? ????????? ??????????????????. (ByBit)";
                                    log.error(errorMsg);
                                    var slackNotificationLog =  slackNotificationLogRepository.save(
                                            new SlackNotificationLog(
                                                    null,
                                                    slackNotification,
                                                    targetSetRequestPath,
                                                    targetSetMethodName,
                                                    errorMsg,
                                                    stringWriter.toString(),
                                                    LocalDateTime.now()
                                            )
                                    );
                                    SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                }
                            } else {
                                var errorMsg = "8-fail. " + memberApi.member.getApis() + " ?????? ?????? ????????? ????????? ??????????????????. (ByBit)";
                                log.error(errorMsg);
                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                        new SlackNotificationLog(
                                                null,
                                                slackNotification,
                                                targetSetRequestPath,
                                                targetSetMethodName,
                                                errorMsg,
                                                "",
                                                LocalDateTime.now()
                                        )
                                );
                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                            }
                        }
                );
            }
        }

        log.info("--------- [??????]");
        result.setTraces(resultTraceList);
        return result;
    }

    /**
     * ?????? ?????? ?????? ??? ?????? ?????? ??????
     * @param minuteBong ???
     * @return List<Trace>
     */
    @Transactional
    public TraceExitSetResult traceExitSet(Integer minuteBong) {

        var objectMapper = new ObjectMapper();

        log.info("1. result: ????????? Return ????????? ?????? ??????, resultTraceList: ????????? ??? ?????? Return ????????? ?????? ??????");
        var resultTraceList = new ArrayList<Trace>();
        var result = new TraceExitSetResult();

        log.info("Slack ?????? ????????? ?????? ??????");
        var slackNotificationOptional = slackNotificationRepository.findById(1);
        if (slackNotificationOptional.isEmpty()){
            result.setResult(EXIT_RESULT.NO_SLACK);
            return result;
        }
        var slackNotification = slackNotificationOptional.get();


        log.info("2. traces: DB ????????? ????????? ?????? ?????? ????????? ??????");
        var traces = traceRepository.findByStartFlagAndMinuteBong(false, minuteBong);

        log.info("3. ??? ??????,?????? ????????? ??????, ?????? ?????? ????????? ????????????");
        var bongBaseOptional = bongBaseRepository.findByMinuteBong(minuteBong);
        if (bongBaseOptional.isEmpty()){
            var errorMsg = "3-fail. " + minuteBong + "?????? ???????????? ????????????.";
            log.error(errorMsg);
            var slackNotificationLog =  slackNotificationLogRepository.save(
                    new SlackNotificationLog(
                            null,
                            slackNotification,
                            exitSetRequestPath,
                            exitSetMethodName,
                            errorMsg,
                            "",
                            LocalDateTime.now()
                    )
            );
            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);

            result.setResult(EXIT_RESULT.NO_BONG_TARGET_BASE);
            return result;
        }

        log.info("3-1. ?????? ?????? ????????? ?????? ??????");
        if (bongBaseOptional.get().getExitRates().size() == 0) {
            var errorMsg = "3-1-fail. "+ minuteBong + "?????? ????????? ?????? ???????????? ????????????.";
            log.error(errorMsg);
            var slackNotificationLog =  slackNotificationLogRepository.save(
                    new SlackNotificationLog(
                            null,
                            slackNotification,
                            exitSetRequestPath,
                            exitSetMethodName,
                            errorMsg,
                            "",
                            LocalDateTime.now()
                    )
            );
            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
            result.setResult(EXIT_RESULT.NO_BONG_EXIT_BASE);
            return result;
        }

        log.info("bongBase: ??????, ?????? ?????? ?????????");
        var bongBase = bongBaseOptional.get();

        traces.forEach(
                trace -> {

                    log.info("4. ??????????????? ?????? ????????? ???????????? ?????? ?????? (Sort ??????) - [???????????? ??????]");
                    var exitRates = bongBase
                            .getExitRates()
                            .stream()
                            .sorted(Comparator.comparing(BongBaseExitRate::getSort))
                            .collect(Collectors.toList());

                    var isBuy   = trace.isBuyFlag();
                    var member  = trace.getMember();

                    log.info("5. ????????? ????????? Api key??? ???????????? ?????? ??????");
                    var memberApiOptional = memberApiRepository.findByMinuteBongAndMemberIdx(minuteBong, member.getIdx());

                    if (memberApiOptional.isPresent()) {

                        var memberApi = memberApiOptional.get();

                        log.info("6. [Bybit Rest] ?????? ?????? ?????????");
                        log.info("??????????????? ????????? ???????????? ??????????????? - ????????? ??? ???????????? ?????????????????? ???????????? ????????? ???????????????");
                        log.info("Filled - ????????? ??? ?????? ???");
                        log.info("PartiallyFilled - ????????? ??????");
                        var responseOrderList = BybitOrderUtil.order_list(
                                memberApi.getApiKey(),
                                memberApi.getSecretKey(),
                                ORDER_STATUS.Filled + "," + ORDER_STATUS.PartiallyFilled
                        );

                        if (responseOrderList != null && responseOrderList.getStatusCode().equals(HttpStatus.OK) && responseOrderList.getBody() != null) {

                            try {
                                var bybitMyOrder = objectMapper.readValue(responseOrderList.getBody().toString(), BybitMyOrder.class);
                                if (!bybitMyOrder.getRet_code().equals(0)) {
                                    var errorMsg = "6-fail. [Bybit Rest] ?????? ?????? ????????? ?????? ?????? Ret_code: " + bybitMyOrder.getRet_code() + "(" +bybitMyOrder.getRet_msg() + ")";
                                    log.error(errorMsg);
                                    var slackNotificationLog =  slackNotificationLogRepository.save(
                                            new SlackNotificationLog(
                                                    null,
                                                    slackNotification,
                                                    exitSetRequestPath,
                                                    exitSetMethodName,
                                                    errorMsg,
                                                    "",
                                                    LocalDateTime.now()
                                            )
                                    );
                                    SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                } else {

                                    var bybitMyOrderResult = bybitMyOrder.getResult();
                                    var myOrderDataList = bybitMyOrderResult.getData();

                                    if (myOrderDataList == null || myOrderDataList.size() == 0) {
                                        var errorMsg = "6-fail-3. [Bybit Rest] ????????? ?????? ???????????? ????????????.";
                                        log.error(errorMsg);
                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                new SlackNotificationLog(
                                                        null,
                                                        slackNotification,
                                                        exitSetRequestPath,
                                                        exitSetMethodName,
                                                        errorMsg,
                                                        "",
                                                        LocalDateTime.now()
                                                )
                                        );
                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                    } else {
                                        log.info("7. ???????????? ?????? ?????? ??????");
                                        var traceEnterList = trace.getTraceEnters();
                                        if(traceEnterList.size() == 0) {
                                            var errorMsg = "7-fail. " + trace.getIdx() + " ?????? ?????? ???????????? ???????????? ????????????. Trace Idx(" + trace.getIdx() + ") ????????? ???????????? ????????????.";
                                            log.error(errorMsg);
                                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                                    new SlackNotificationLog(
                                                            null,
                                                            slackNotification,
                                                            exitSetRequestPath,
                                                            exitSetMethodName,
                                                            errorMsg,
                                                            "",
                                                            LocalDateTime.now()
                                                    )
                                            );
                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                        } else {
                                            log.info("9. Bybit ????????? ???????????? DB??? ???????????? ??????");
                                            log.info("myOrderDataListFilled: ?????? ?????? ????????? ?????????");
                                            var myOrderDataListFilled
                                                    = myOrderDataList.stream()
                                                        .filter(data -> data.getOrder_status().equals(ORDER_STATUS.Filled))
                                                        .distinct()
                                                        .collect(Collectors.toList());


                                            log.info("myOrderDataListPartiallyFilled: ?????? ?????? ??????????????? ?????????");
                                            var myOrderDataListPartiallyFilled
                                                    = myOrderDataList.stream()
                                                        .filter(data -> data.getOrder_status().equals(ORDER_STATUS.PartiallyFilled))
                                                        .distinct()
                                                        .collect(Collectors.toList());

                                            var deleteTraceEnter = new ArrayList<TraceEnter>();
                                            traceEnterList.forEach(
                                                    traceEnter -> {
                                                        if (!myOrderDataListFilled.stream()
                                                                .map(BybitMyOrderData::getOrder_id)
                                                                .collect(Collectors.toList())
                                                                .contains(traceEnter.getOrderId())) {

                                                            log.info("?????? ????????? ??????????????? ??????");
                                                            log.info("10. ?????? ????????? ??? ??????");
                                                            if (myOrderDataListPartiallyFilled.stream()
                                                                    .map(BybitMyOrderData::getOrder_id)
                                                                    .collect(Collectors.toList())
                                                                    .contains(traceEnter.getOrderId())) {

                                                                var myOrderDataMatchList = myOrderDataListPartiallyFilled
                                                                        .stream()
                                                                        .filter(bybitMyOrderData -> bybitMyOrderData.getOrder_id().equals(traceEnter.getOrderId()))
                                                                        .distinct()
                                                                        .collect(Collectors.toList());

                                                                if (myOrderDataMatchList.size() != 1 ) {
                                                                    var dataMsg = new StringBuilder();
                                                                    var idx = 1;
                                                                    for (var orderData : myOrderDataMatchList) {
                                                                        dataMsg.append(idx);
                                                                        dataMsg.append(":");
                                                                        dataMsg.append(orderData.toString());
                                                                        dataMsg.append(",  ");
                                                                        idx += 1;
                                                                    }
                                                                    var errorMsg = "10-fail. MyOrderData??? ???????????? ????????? ????????????. " + dataMsg;
                                                                    log.error(errorMsg);
                                                                    var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                            new SlackNotificationLog(
                                                                                    null,
                                                                                    slackNotification,
                                                                                    exitSetRequestPath,
                                                                                    exitSetMethodName,
                                                                                    errorMsg,
                                                                                    "",
                                                                                    LocalDateTime.now()
                                                                            )
                                                                    );
                                                                    SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                } else {

                                                                    var myOrderDataMatch = myOrderDataMatchList.get(0);

                                                                    log.info("11. [Bybit] ?????? ???????????? ?????? ??????");
                                                                    var orderCancelResponse = BybitOrderUtil.order_cancel (
                                                                            memberApi.getApiKey(),
                                                                            memberApi.getSecretKey(),
                                                                            myOrderDataMatch.getOrder_id()
                                                                    );

                                                                    if (orderCancelResponse == null || (!HttpStatus.OK.equals(orderCancelResponse.getStatusCode())) || orderCancelResponse.getBody() == null) {
                                                                        var errorMsg = "11-fail. " + memberApi.getApiKey() + "," +  myOrderDataMatch.getOrder_id() + " ?????? ?????? ??????";
                                                                        log.error(errorMsg);
                                                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                                new SlackNotificationLog(
                                                                                        null,
                                                                                        slackNotification,
                                                                                        exitSetRequestPath,
                                                                                        exitSetMethodName,
                                                                                        errorMsg,
                                                                                        null,
                                                                                        LocalDateTime.now()
                                                                                )
                                                                        );
                                                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                    } else {

                                                                        try {
                                                                            var bybitMyOrderCancel = objectMapper.readValue(orderCancelResponse.getBody().toString(), BybitMyOrderCancel.class);
                                                                            if (bybitMyOrderCancel.getRet_code() != 0) {
                                                                                var errorMsg = "11-fail-2. " + memberApi.getApiKey() + "," + myOrderDataMatch.getOrder_id() + " ?????? ?????? ?????? (Bybit) Ret_code: " + bybitMyOrderCancel.getRet_code() + "(" + bybitMyOrderCancel.getRet_msg() + ")";
                                                                                log.error(errorMsg);
                                                                                var slackNotificationLog = slackNotificationLogRepository.save(
                                                                                        new SlackNotificationLog(
                                                                                                null,
                                                                                                slackNotification,
                                                                                                exitSetRequestPath,
                                                                                                exitSetMethodName,
                                                                                                errorMsg,
                                                                                                "",
                                                                                                LocalDateTime.now()
                                                                                        )
                                                                                );
                                                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                            } else {
                                                                                // TODO
                                                                                // ?????? ?????? ?????? ????????? ??? ?????? ????????? ??????????????? ???????????????
                                                                                // ?????? ???????????? ????????? ???????????? ???????????? ??????????????????
                                                                                log.info("12. ?????? ?????? ????????? ?????? ?????? ?????? DB Update");
                                                                                traceEnter.setting(bybitMyOrderCancel.getResult());
                                                                            }
                                                                        } catch (JsonProcessingException e) {
                                                                            var stringWriter = new StringWriter();
                                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                                            var errorMsg = "11-fail-JsonProcessingException. " + memberApi.getApiKey() + "," + myOrderDataMatch.getOrder_id() + " ?????? ?????? ????????? ?????? (Bybit)";
                                                                            log.error(errorMsg);
                                                                            var slackNotificationLog = slackNotificationLogRepository.save(
                                                                                    new SlackNotificationLog(
                                                                                            null,
                                                                                            slackNotification,
                                                                                            exitSetRequestPath,
                                                                                            exitSetMethodName,
                                                                                            errorMsg,
                                                                                            stringWriter.toString(),
                                                                                            LocalDateTime.now()
                                                                                    )
                                                                            );
                                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                                        }
                                                                    }
                                                                }

                                                            } else {
                                                                log.info("13. ?????? ????????? ?????? ???????????? ??????");
                                                                deleteTraceEnter.add(traceEnter);
                                                            }



                                                        } else {

                                                            log.info("?????? ????????? ??????????????? ???");
                                                            log.info("14. DB <-> Bybit ???????????? ????????? ?????? ??? ????????? ????????????");
                                                            var myOrderDataMatchList = myOrderDataListFilled
                                                                    .stream()
                                                                    .filter(bybitMyOrderData -> bybitMyOrderData.getOrder_id().equals(traceEnter.getOrderId()))
                                                                    .distinct()
                                                                    .collect(Collectors.toList());

                                                            if (myOrderDataMatchList.size() != 1 ) {
                                                                var dataMsg = new StringBuilder();
                                                                var idx = 1;
                                                                for (var orderData : myOrderDataMatchList) {
                                                                    dataMsg.append(idx);
                                                                    dataMsg.append(":");
                                                                    dataMsg.append(orderData.toString());
                                                                    dataMsg.append(",  ");
                                                                    idx += 1;
                                                                }
                                                                log.error("14-fail. MyOrderData??? ???????????? ????????? ????????????. " + dataMsg);
                                                            } else {
                                                                log.info("15. DB ?????????");
                                                                var myOrderDataMatch = myOrderDataMatchList.get(0);
                                                                traceEnter.setting(myOrderDataMatch);
                                                            }
                                                        }
                                                    }
                                            );

                                            log.info("16. ?????? ????????? ?????? ???????????? ?????? (DB)");
                                            traceEnterRepository.deleteAll(deleteTraceEnter);


                                            log.info("17. ?????? ?????? ????????? ??????????????? ??????");
                                            if (trace.getTraceEnters().stream().anyMatch(traceEnter -> !traceEnter.getOrderStatus().equals(ORDER_STATUS.Filled))) {
                                                var errorMsg = "17-fail. ?????? ?????? [?????? ????????? ???????????? ??????] Trace Idx: " + trace.getIdx();
                                                log.error(errorMsg);
                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                        new SlackNotificationLog(
                                                                null,
                                                                slackNotification,
                                                                exitSetRequestPath,
                                                                exitSetMethodName,
                                                                errorMsg,
                                                                "",
                                                                LocalDateTime.now()
                                                        )
                                                );
                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                            } else {

                                                log.info("8. ?????? ???????????? ??? ??????, ?????? ????????? ??????, ?????? ?????? ????????? ??????");
                                                trace.setLossRate(bongBase.getLossRate());
                                                trace.setEnterEndRate(bongBase.getEnterEndRate());

                                                log.info("[????????? ????????????]");
                                                log.info("Buy  = ????????? + ( ????????? - ??????(??????) )");
                                                log.info("Sell = ????????? - ( ??????(??????) - ????????? )");
                                                var price       = trace.getPrice();     /* ????????? (????????????) */
                                                var basePrice   = trace.getBasePrice(); /* ??????(??????, ??????) */
                                                var targetPrice = isBuy ? price + (price - basePrice) : price - (basePrice - price); /* ????????? */

                                                log.info("[?????? ?????? ????????????]");
                                                log.info("????????? = ?????? + (????????? - ??????) * ??????");
                                                trace.setLossPrice(basePrice + (targetPrice - basePrice) * trace.getLossRate());


                                                log.info("18. ?????? ????????? qty: ?????? ?????? ????????? (??????)");
                                                log.info("addQty: ???????????? ???????????? ??? qty ?????? ????????? ??? ??????????????? ?????? qty ??? ????????? ?????? ??????");
                                                int totalQty = traceEnterList.stream().map(TraceEnter::getQty).reduce(0, Integer::sum);
                                                var addQty = 0;

                                                var saveTraceRateList = new ArrayList<TraceExitRate>();

                                                for ( var exitRate : exitRates ) {

                                                    log.info("?????? exitQty");
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

                                                    log.info("[?????? ??????, ?????? ?????? ????????????]");
                                                    log.info("????????? = ?????? + (????????? - ??????) * ??????");
                                                    var exitPrice = basePrice + (targetPrice - basePrice) * exitRate.getExitRate();
                                                    var lossPrice = basePrice + (targetPrice - basePrice) * exitRate.getLossRate();

                                                    log.info("19. [Bybit Rest] ?????? ?????????" );
                                                    log.info("Buy -> Sell, Sell -> Buy ????????? ???????????????");
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
                                                        var errorMsg = "19-fail. ?????? ????????? ?????? ??????" + memberApi.getMember() + ", exitQty: " + exitQty + "exitPrice: " + exitPrice + " ????????? ??????????????????. (Bybit)";
                                                        log.error(errorMsg);
                                                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                new SlackNotificationLog(
                                                                        null,
                                                                        slackNotification,
                                                                        exitSetRequestPath,
                                                                        exitSetMethodName,
                                                                        errorMsg,
                                                                        "",
                                                                        LocalDateTime.now()
                                                                )
                                                        );
                                                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                    } else {

                                                        try {
                                                            var bybitOrder = objectMapper.readValue(responseEntity.getBody().toString(), BybitOrder.class);
                                                            if (bybitOrder.getRet_code() != 0) {
                                                                var errorMsg = "19-fail-2. ?????? ????????? ??????" + memberApi.getMember() + ", exitQty: "
                                                                        + exitQty + "exitPrice: " + exitPrice + " ????????? ??????????????????. (Bybit)  Ret_code: "
                                                                        + bybitOrder.getRet_code() + "(" +bybitOrder.getRet_msg() + ")";
                                                                log.error(errorMsg);
                                                                var slackNotificationLog =  slackNotificationLogRepository.save(
                                                                        new SlackNotificationLog(
                                                                                null,
                                                                                slackNotification,
                                                                                exitSetRequestPath,
                                                                                exitSetMethodName,
                                                                                errorMsg,
                                                                                "",
                                                                                LocalDateTime.now()
                                                                        )
                                                                );
                                                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                            } else {

                                                                log.info("20. ?????? ?????? ?????????????????? [DB] ??????");
                                                                var traceExitParam = new TraceExit(bybitOrder.getResult(), exitRate.getSort());
                                                                var traceExit = traceExitRepository.save(traceExitParam);

                                                                log.info("21. ?????? ?????? ?????? ??????");
                                                                traceExit.setStopLossPrice(lossPrice);
                                                                traceExit.setTrace(trace);
                                                                trace.getTraceExits().add(traceExit);

                                                                log.info("22. ?????? ?????? ?????? ?????? ?????? ??????");
                                                                saveTraceRateList.add(
                                                                        new TraceExitRate(
                                                                                null,
                                                                                null, exitRate.getExitRate(),
                                                                                exitRate.getTraceRate(),
                                                                                exitRate.getLossRate(),
                                                                                exitRate.getSort(),
                                                                                LocalDateTime.now(),
                                                                                null
                                                                        )
                                                                );
                                                            }

                                                        } catch (JsonProcessingException e){
                                                            var stringWriter = new StringWriter();
                                                            e.printStackTrace(new PrintWriter(stringWriter));
                                                            var errorMsg = "19-fail-JsonProcessingException." + memberApi.getMember() + ", exitQty: " + exitQty + "exitPrice: " + exitPrice + " ????????? ??????????????????. Json ?????? ?????? (Bybit)";
                                                            log.error(errorMsg);
                                                            var slackNotificationLog = slackNotificationLogRepository.save(
                                                                    new SlackNotificationLog(
                                                                            null,
                                                                            slackNotification,
                                                                            exitSetRequestPath,
                                                                            exitSetMethodName,
                                                                            errorMsg,
                                                                            stringWriter.toString(),
                                                                            LocalDateTime.now()
                                                                    )
                                                            );
                                                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                                                        }
                                                    }
                                                }

                                                log.info("23. ?????? ????????? ????????? ?????? ??? ?????? ?????? [DB]");
                                                if (saveTraceRateList.size() > 0) {
                                                    var traceExitRateList = traceExitRateRepository.saveAll(saveTraceRateList);
                                                    traceExitRateList.forEach(traceExitRate -> {
                                                        traceExitRate.setTrace(trace);
                                                        trace.getTraceExitsRates().add(traceExitRate);
                                                    });
                                                }


                                                log.info("24. ?????? ????????? ????????? ?????? ?????? ????????? ?????? ??????");
                                                trace.setStartFlag(true);
                                                resultTraceList.add(trace);

                                            }
                                        }
                                    }
                                }
                            } catch (JsonProcessingException e) {
                                var stringWriter = new StringWriter();
                                e.printStackTrace(new PrintWriter(stringWriter));
                                var errorMsg = "6-fail-JsonProcessingException. [Bybit Rest] ?????? ?????? ????????? ????????? ?????? ??????: " + memberApi.getApiKey();
                                log.error(errorMsg);
                                var slackNotificationLog = slackNotificationLogRepository.save(
                                        new SlackNotificationLog(
                                                null,
                                                slackNotification,
                                                exitSetRequestPath,
                                                exitSetMethodName,
                                                errorMsg,
                                                stringWriter.toString(),
                                                LocalDateTime.now()
                                        )
                                );
                                SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                            }
                        } else {
                            var errorMsg = "6-fail-2. [Bybit Rest] ?????? ?????? ????????? ?????? ??????";
                            log.error(errorMsg);
                            var slackNotificationLog =  slackNotificationLogRepository.save(
                                    new SlackNotificationLog(
                                            null,
                                            slackNotification,
                                            exitSetRequestPath,
                                            exitSetMethodName,
                                            errorMsg,
                                            "",
                                            LocalDateTime.now()
                                    )
                            );
                            SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                        }

                    } else {
                        var errorMsg = "5-fail. ????????? Api Key ??? ?????? ??? ????????????." + member.getIdx();
                        log.error(errorMsg);
                        var slackNotificationLog =  slackNotificationLogRepository.save(
                                new SlackNotificationLog(
                                        null,
                                        slackNotification,
                                        exitSetRequestPath,
                                        exitSetMethodName,
                                        errorMsg,
                                        "",
                                        LocalDateTime.now()
                                )
                        );
                        SlackNotificationUtil.send(errorMsg, slackNotification.getUrl(), slackNotificationLog);
                    }
                }
        );

        result.setTraces(resultTraceList);
        return result;
    }
}
