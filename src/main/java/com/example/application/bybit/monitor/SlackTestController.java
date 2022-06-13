package com.example.application.bybit.monitor;

import com.example.application.bybit.trace.repository.SlackNotificationRepository;
import com.example.application.bybit.util.SlackNotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class SlackTestController {

    private final SlackNotificationRepository slackNotificationRepository;


    @PostMapping("/slack")
    public void testslack(){
        var url = slackNotificationRepository.findById(1);
        var slack = new SlackNotificationUtil();
        slack.send("테스트 메셎지",url.get().getUrl());
    }
}
