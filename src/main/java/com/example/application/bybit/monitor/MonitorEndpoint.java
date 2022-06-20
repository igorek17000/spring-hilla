package com.example.application.bybit.monitor;

import com.example.application.bybit.auth.UserDetailsImpl;
import com.example.application.bybit.common.SlackNotificationLog;
import com.example.application.bybit.common.SlackNotificationLogRepository;
import com.example.application.bybit.common.SlackNotificationRepository;
import com.example.application.bybit.monitor.dto.BalanceItem;
import com.example.application.bybit.monitor.dto.ExecuteItem;
import com.example.application.bybit.monitor.dto.PnlItem;
import com.example.application.bybit.util.BybitEncryption;
import com.example.application.bybit.util.BybitOrderUtil;
import com.example.application.bybit.util.SlackNotificationUtil;
import com.example.application.member.MemberApi;
import com.example.application.member.MemberApiRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import dev.hilla.Endpoint;
import dev.hilla.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.client.RestTemplate;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.*;

@Endpoint
@AnonymousAllowed
@RequiredArgsConstructor
@Slf4j
public class MonitorEndpoint {

    private final MemberApiRepository memberApiRepository;

    private final SlackNotificationRepository slackNotificationRepository;
    private final SlackNotificationLogRepository slackNotificationLogRepository;
    private ObjectMapper om = new ObjectMapper();

    //-----------------------------------------BALANCE--------------------------------------------
    @PermitAll
    public @Nonnull List<@Nonnull BalanceItem> getBalance() throws JsonProcessingException{
        var principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        var balanceItems = new ArrayList<BalanceItem>();
        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findByMemberUsername(principal.getUsername());

        for (MemberApi api:member
        ) {
            // BTC
            var response = BybitOrderUtil.my_wallet(api.getApiKey(),api.getSecretKey());
            if(response == null || !response.getStatusCode().equals(HttpStatus.OK) || response.getBody() == null){
                log.info("Monitoring Web : 잔액 조회 실패");

                var slackNotificationOptional = slackNotificationRepository.findById(1);

                if (slackNotificationOptional.isPresent()) {
                    var slackNotification = slackNotificationOptional.get();
                    var slackMsg = "Monitoring Web - balance get fail , member api id : "+api.getIdx();
                    var slackNotificationLog = slackNotificationLogRepository.save(
                            new SlackNotificationLog(
                                    null,
                                    slackNotification,
                                    "/balance",
                                    "getBalance",
                                    slackMsg,
                                    "",
                                    LocalDateTime.now()
                            )
                    );
                    SlackNotificationUtil.send(slackMsg, slackNotification.getUrl(), slackNotificationLog);
                }

            }else{
                HashMap<String,Map<String, Map<String,Double>>> body = om.readValue(response.getBody().toString(), HashMap.class);
                var decimal = BigDecimal.valueOf(body.get("result").get("BTC").get("equity"));

                // BTC to USD
                var usdResponse = restTemplate.getForEntity("https://api.bybit.com/v2/public/tickers?symbol=BTCUSD",String.class);
                HashMap<String,List<Map<String,String>>> usd= om.readValue(usdResponse.getBody(),HashMap.class);
                var usdToDecimal = new BigDecimal(usd.get("result").get(0).get("last_price"));
                var btcToUsd = BigDecimal.valueOf(usdToDecimal.doubleValue() * decimal.doubleValue());

                // USD to WON
                var wonResponse = restTemplate.getForEntity("https://quotation-api-cdn.dunamu.com/v1/forex/recent?codes=FRX.KRWUSD",String.class);
                List<HashMap<String,Object>> won = om.readValue(wonResponse.getBody(),List.class);
                var usdToWon = new BigDecimal(won.get(0).get("basePrice").toString());
                DecimalFormat decFormat = new DecimalFormat("###,###");

                balanceItems.add(new BalanceItem(
                        api.getMinuteBong(),
                        decFormat.format(btcToUsd.multiply(usdToWon).doubleValue()),
                        decimal,
                        btcToUsd.setScale(3, RoundingMode.HALF_UP)
                ));
            }

        }
        return balanceItems;
    }

    //-----------------------------------------CLOSED P&L--------------------------------------------
    public @Nonnull List<@Nonnull PnlItem> getPnls(int minute) throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        var principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        // TODO DB 데이터 쌓이면 TRACE 데이터로 수정
        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findByMinuteBongAndMemberUsername(minute, principal.getUsername());

        List<PnlItem> pnls = new ArrayList<>();

        if(member.isPresent()) {

            var memberApi = member.get();

            var map = new TreeMap<String, String>(
                    new Comparator<String>() {
                        public int compare(String obj1, String obj2) {
                            return obj1.compareTo(obj2);
                        }
                    });

            map.put("symbol", "BTCUSD");
            map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli() + "");
            map.put("api_key", memberApi.getApiKey());

            var entity = new HttpEntity<>(map);

            var queryString = BybitEncryption.genQueryString(map, memberApi.getSecretKey());
            var url = "https://api.bybit.com/v2/private/execution/list?" + queryString;

            var response = restTemplate.getForEntity(url, String.class, entity);
            HashMap<String, Map<String, List<Map<String, Object>>>> body = om.readValue(response.getBody(), HashMap.class);

            var list = body.get("result").get("trade_list");
            for (var m : list
            ) {
                if (m.get("exec_type").equals("Trade")) {
                    PnlItem item = new PnlItem(
                            minute,
                            m.get("symbol").toString(),
                            "undefined",
                            m.get("order_qty").toString(),
                            m.get("order_price").toString(),
                            "undefined",
                            "undefined",
                            m.get("exec_type").toString(),
                            m.get("exec_time").toString()
                    );
                    pnls.add(item);
                }
            }
        }
        return pnls;
    }

    //-----------------------------------------EXECUTION LIST--------------------------------------------
    @PermitAll
    public @Nonnull List<@Nonnull ExecuteItem> getExecute(int minute) throws JsonProcessingException {
        var principal = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        var member = memberApiRepository.findByMinuteBongAndMemberUsername(minute,principal.getUsername());

        List<ExecuteItem> executeItems = new ArrayList<>();

        if(member.isPresent()) {

            var memberApi = member.get();

            var response = BybitOrderUtil.execution_list(memberApi.getApiKey(),memberApi.getSecretKey());

            if(response == null || !response.getStatusCode().equals(HttpStatus.OK) || response.getBody() == null){
                log.info("Monitoring Web : 거래 내역 조회 실패");

                var slackNotificationOptional = slackNotificationRepository.findById(1);

                if (slackNotificationOptional.isPresent()) {
                    var slackNotification = slackNotificationOptional.get();
                    var slackMsg = "Monitoring Web - execute list get fail , member api id : "+memberApi.getIdx();
                    var slackNotificationLog = slackNotificationLogRepository.save(
                            new SlackNotificationLog(
                                    null,
                                    slackNotification,
                                    "/execute",
                                    "getExecute",
                                    slackMsg,
                                    "",
                                    LocalDateTime.now()
                            )
                    );
                    SlackNotificationUtil.send(slackMsg, slackNotification.getUrl(), slackNotificationLog);
                }

            }else{
                HashMap<String, Map<String, List<Map<String, Object>>>> body = om.readValue(response.getBody().toString(), HashMap.class);

                var list = body.get("result").get("trade_list");
                var decFormat = new DecimalFormat("###,###");
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                for (var m : list
                ) {
                    if (m.get("exec_type").equals("Trade")) {
                        ExecuteItem item = new ExecuteItem(
                                minute,
                                m.get("side").toString(),
                                m.get("symbol").toString(),
                                decFormat.format(Double.parseDouble(m.get("exec_qty").toString())),
                                decFormat.format(Double.parseDouble(m.get("exec_price").toString())),
                                m.get("exec_type").toString(),
                                format.format(new Date(Long.parseLong(m.get("exec_time").toString())*1000))
                        );
                        executeItems.add(item);
                    }
                }
            }
        }
        return executeItems;
    }
}
