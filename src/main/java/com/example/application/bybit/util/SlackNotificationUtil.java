package com.example.application.bybit.util;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class SlackNotificationUtil {

    public static void send(String message, String channel_url){
        var restTemplate = new RestTemplate();

        var request = new HashMap<String,Object>();

        request.put("username", "gangmii"); // slack에 표시되는 username
        request.put("text", message);

        var entity = new HttpEntity<Map<String,Object>>(request);

        restTemplate.exchange(channel_url, HttpMethod.POST, entity, String.class);
    }

}
