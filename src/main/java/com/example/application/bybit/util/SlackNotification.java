package com.example.application.bybit.util;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class SlackNotification {

    public void send(){
        var restTemplate = new RestTemplate();

        var request = new HashMap<String,Object>();

        request.put("username", "gangmii"); // slack에 표시되는 username
        request.put("text", "테스트");

        var entity = new HttpEntity<Map<String,Object>>(request);

        String url = "https://hooks.slack.com/services/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"; // slack app에 등록후 발급받은 hook url

        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

    }

}
