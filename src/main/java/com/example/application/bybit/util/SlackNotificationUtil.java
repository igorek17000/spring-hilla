package com.example.application.bybit.util;

import com.example.application.bybit.common.SlackNotificationLog;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class SlackNotificationUtil {

    public static void send(String message, String channel_url, SlackNotificationLog slackNotificationLog){
        var restTemplate = new RestTemplate();

        var request = new HashMap<String,Object>();

        request.put("username", "gangmii"); // slack에 표시되는 username
        request.put("text", message + " -- 상세 로그 확인 IDX [" + slackNotificationLog.getIdx() + "]");

        var entity = new HttpEntity<Map<String,Object>>(request);

        restTemplate.exchange(channel_url, HttpMethod.POST, entity, String.class);
    }

}
