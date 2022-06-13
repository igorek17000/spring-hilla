package com.example.application.bybit.util;

import com.example.application.bybit.trace.repository.SlackNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class SlackNotificationUtil {

    public void send(String message, String channel_url){

        var restTemplate = new RestTemplate();

        var request = new HashMap<String,Object>();

        request.put("username", "gangmii"); // slack에 표시되는 username
        request.put("text", message);

        var entity = new HttpEntity<Map<String,Object>>(request);

        String url = channel_url; // slack app에 등록후 발급받은 hook url

        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

    }

}
