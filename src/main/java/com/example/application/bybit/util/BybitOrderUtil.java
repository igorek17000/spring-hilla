package com.example.application.bybit.util;

import com.example.application.bybit.trace.enums.ORDER_TYPE;
import com.example.application.bybit.trace.enums.SIDE;
import com.example.application.bybit.trace.enums.TIME_IN_FORCE;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.TreeMap;

@Slf4j
public class BybitOrderUtil {
    private final static String order_url= "https://api-testnet.bybit.com/v2/private/order/create?";
    private final static String position_url = "https://api.bybit.com/v2/private/position/list?";
    private final static String order_list_url = "https://api.bybit.com/v2/private/order/list?";
    private final static String order_cancel_url = "https://api.bybit.com/v2/private/order/cancel?";


    // TODO stop_loss 손절금액 설정있던데 확인 필요
    /**
     * <p>공매수, 공매도</p>
     * <a href="https://bybit-exchange.github.io/docs/inverse/#t-placeactive"> 참고 링크 </a>
     */
    public static ResponseEntity<?> order (
            String apiKey,
            String secretKey,
            Integer qty,
            SIDE side,
            TIME_IN_FORCE time_in_force,
            Integer price,
            ORDER_TYPE order_type
    ) {

        var map = new TreeMap<String, String>(Comparator.naturalOrder());
        map.put("symbol", "BTCUSD");
        map.put("qty",  String.valueOf(qty));
        map.put("order_type", order_type.toString()); // Market 시장가
        map.put("side", side.toString());
        map.put("time_in_force", time_in_force.toString());
        map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli() + "");
        map.put("api_key", apiKey);

        if (time_in_force.equals(TIME_IN_FORCE.PostOnly)) {
            map.put("price", String.valueOf(price));
        }

        try {
            var queryString = BybitEncryption.genQueryString(map, secretKey);

            var template = new RestTemplate();
            var response = template.postForEntity(
                    order_url + queryString,
                    null,
                    String.class
            );

            var body = response.getBody();

            log.info("order: " + response.getStatusCode());
            log.info("order: " + body);

            return response;

        } catch (NoSuchAlgorithmException | InvalidKeyException e){
            e.printStackTrace();

            // Slack 알림

            return null;
        }
    }

    /**
     * <p>주문취소</p>
     * <a href="https://bybit-exchange.github.io/docs/inverse/#t-cancelactive"> 참고 링크 </a>
     */
    public static ResponseEntity<?> order_cancel (
            String apiKey,
            String secretKey,
            String order_id
    ) {

        var map = new TreeMap<String, String>(Comparator.naturalOrder());
        map.put("symbol", "BTCUSD");
        map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli() + "");
        map.put("api_key", apiKey);
        map.put("order_id", order_id);

        try {
            var queryString = BybitEncryption.genQueryString(map, secretKey);

            var template = new RestTemplate();
            var response = template.postForEntity(
                    order_cancel_url + queryString,
                    null,
                    String.class
            );

            var body = response.getBody();

            log.info("order_cancel: " + response.getStatusCode());
            log.info("order_cancel: " + body);

            return response;

        } catch (NoSuchAlgorithmException | InvalidKeyException e){
            e.printStackTrace();

            // Slack 알림

            return null;
        }
    }

    /**
     * <p>나의 현재 주문 현황</p>
     * <a href="https://bybit-exchange.github.io/docs/inverse/#t-getactive"> 참고 링크 </a>
     */
    public static ResponseEntity<?> order_list(
            String apiKey,
            String secretKey,
            String order_status
    ) {

        var map = getMappingParamTreeMap(apiKey);
        map.put("order_status", order_status);

        try {
            var queryString = BybitEncryption.genQueryString(map, secretKey);

            var template = new RestTemplate();
            var response = template.getForEntity(
                    order_list_url + queryString,
                    String.class
            );

            var body = response.getBody();

            log.info("order_list: " + response.getStatusCode());
            log.info("order_list: " + body);

            return response;

        } catch (NoSuchAlgorithmException | InvalidKeyException e){
            e.printStackTrace();

            // Slack 알림

            return null;
        }
    }

    /**
     * <p> 나의 현재 포지션 현황</p>
     * <a href="https://bybit-exchange.github.io/docs/inverse/#t-myposition"> 참고 링크 </a>
     */
    public static ResponseEntity<?> position (
            String apiKey,
            String secretKey
    ) {
        var map = getMappingParamTreeMap(apiKey);
        try {
            var queryString = BybitEncryption.genQueryString(map, secretKey);

            var template = new RestTemplate();
            var response = template.getForEntity(
                    position_url + queryString,
                    String.class
            );

            var body = response.getBody();

            log.info("position: " + response.getStatusCode());
            log.info("position: " + body);

            return response;

        } catch (NoSuchAlgorithmException | InvalidKeyException e){
            e.printStackTrace();

            // Slack 알림

            return null;
        }
    }

    private static TreeMap<String, String> getMappingParamTreeMap(String apiKey) {
        var map = new TreeMap<String, String>(String::compareTo);
        map.put("api_key",   apiKey);
        map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli()+"");
        map.put("symbol",    "BTCUSD");
        return map;
    }

}
