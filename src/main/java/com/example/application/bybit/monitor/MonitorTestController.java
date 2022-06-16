package com.example.application.bybit.monitor;

import com.example.application.bybit.monitor.dto.BalanceItem;
import com.example.application.bybit.monitor.dto.ExecuteItem;
import com.example.application.bybit.monitor.dto.PnlItem;
import com.example.application.bybit.util.BybitEncryption;
import com.example.application.member.MemberApiRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.time.ZonedDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class MonitorTestController {
    //TODO 테스트용 컨트롤러 , 테스트 후 hilla로
    private final MemberApiRepository memberApiRepository;

    private ObjectMapper om = new ObjectMapper();

    @GetMapping("/balance")
    public List<BalanceItem> balance() throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        List<BalanceItem> balanceItems = new ArrayList<>();
        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findByMemberIdx(1);

        for (var api:member
             ) {
            var map = new TreeMap<String, String>(
                    new Comparator<String>() {
                        public int compare(String obj1, String obj2) {
                            return obj1.compareTo(obj2);
                        }
                    });

            map.put("coin", "BTC"); // USD, KRW 는 symbol 없음, 가격 받아서 계산 해야됌
            map.put("timestamp", ZonedDateTime.now().toInstant().toEpochMilli()+"");
            map.put("api_key", api.getApiKey());

            var entity = new HttpEntity<>(map);

            var queryString = BybitEncryption.genQueryString(map, api.getSecretKey());
            var url = "https://api.bybit.com/v2/private/wallet/balance?"+ queryString;

            // BTC
            var response = restTemplate.getForEntity(url,String.class,entity);

//        System.out.println(response.getBody());
            HashMap<String,Map<String,Map<String,Double>>> body = om.readValue(response.getBody(), HashMap.class);
            var decimal = BigDecimal.valueOf(body.get("result").get("BTC").get("equity"));

            // BTC to USD
            var usdResponse = restTemplate.getForEntity("https://api.bybit.com/v2/public/tickers?symbol=BTCUSD",String.class,entity);
            HashMap<String,List<Map<String,String>>> usd= om.readValue(usdResponse.getBody(),HashMap.class);
            var usdToDecimal = new BigDecimal(usd.get("result").get(0).get("last_price"));
            var btcToUsd = BigDecimal.valueOf(usdToDecimal.doubleValue() * decimal.doubleValue());

            // USD to WON
            var wonResponse = restTemplate.getForEntity("https://quotation-api-cdn.dunamu.com/v1/forex/recent?codes=FRX.KRWUSD",String.class,entity);
            List<HashMap<String,Object>> won = om.readValue(wonResponse.getBody(),List.class);
            var usdToWon = new BigDecimal(won.get(0).get("basePrice").toString());

            var decFormat = new DecimalFormat("###,###");

            balanceItems.add(new BalanceItem(
                    api.getMinuteBong(),
                    decFormat.format(btcToUsd.multiply(usdToWon).doubleValue()),
                    decimal,
                    btcToUsd.setScale(3, RoundingMode.HALF_UP)
            ));
        }
        return balanceItems;


    }

    @GetMapping("/list")
    public void list() throws NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {

        var restTemplate = new RestTemplate();
        var member = memberApiRepository.findByMinuteBongAndMemberIdx(1,1);

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
                            1,
                            m.get("symbol").toString(),
                            "undefined",
                            m.get("order_qty").toString(),
                            m.get("order_price").toString(),
                            "undefined",
                            "undefined",
                            "exec_type",
                            new Date(Long.parseLong(m.get("exec_time").toString())).toString()
                    );
                    pnls.add(item);
                }
            }


        }
        // 받은 데이터에서 exec_type -> "FUNDING" 제외, "TRADE"만 뽑으면 됨.
    }


}
