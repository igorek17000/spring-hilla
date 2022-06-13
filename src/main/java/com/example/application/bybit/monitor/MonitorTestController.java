package com.example.application.bybit.monitor;

import com.example.application.bybit.util.BybitEncryption;
import com.example.application.member.MemberApiRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.*;

@RequiredArgsConstructor
public class MonitorTestController {
    //TODO 테스트용 컨트롤러 , 테스트 후 hilla로
    private final MemberApiRepository memberApiRepository;

    public void balance() throws NoSuchAlgorithmException, InvalidKeyException {

        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findById(1);
        var memberApi = member.get();

        var map = new TreeMap<String, String>(
                new Comparator<String>() {
                    public int compare(String obj1, String obj2) {
                        return obj1.compareTo(obj2);
                    }
                });

        map.put("coin", "BTC"); // USD, KRW 는 symbol 없음, 가격 받아서 계산 해야됌
        map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli()+"");
        map.put("api_key", memberApi.getApiKey());

        var entity = new HttpEntity<>(map);

        var queryString = BybitEncryption.genQueryString(map, memberApi.getSecretKey());
        var url = "https://api.bybit.com/v2/private/wallet/balance?"+ queryString;

        var response = restTemplate.getForEntity(url,String.class,entity);
        System.out.println(response);

    }

    public void list() throws NoSuchAlgorithmException, InvalidKeyException {

        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findById(1);
        var memberApi = member.get();

        var map = new TreeMap<String, String>(
                new Comparator<String>() {
                    public int compare(String obj1, String obj2) {
                        return obj1.compareTo(obj2);
                    }
                });

        map.put("symbol", "BTCUSD");
        map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli()+"");
        map.put("api_key", memberApi.getApiKey());

        var entity = new HttpEntity<>(map);

        var queryString = BybitEncryption.genQueryString(map, memberApi.getSecretKey());
        var url = "https://api.bybit.com/v2/private/execution/list?"+queryString;

        var response = restTemplate.getForEntity(url,String.class,entity);
        System.out.println(response);
        // 실 거래내역보다 많이 나옴
    }

}
