package com.example.application.bybit.common;

import com.example.application.bybit.common.SlackNotificationRepository;
import com.example.application.bybit.util.SlackNotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SlackTestController {

    private final SlackNotificationRepository slackNotificationRepository;


    @PostMapping("/slack")
    public void testSlack(){
        var slackNotificationOptional = slackNotificationRepository.findById(1);
        slackNotificationOptional.ifPresent(slackNotification -> SlackNotificationUtil.send("테스트 메세지", slackNotification.getUrl()));
    }
}
