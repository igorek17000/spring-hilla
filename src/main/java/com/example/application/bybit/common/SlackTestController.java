package com.example.application.bybit.common;

import com.example.application.bybit.util.SlackNotificationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class SlackTestController {

    private final SlackNotificationRepository slackNotificationRepository;
    private final SlackNotificationLogRepository slackNotificationLogRepository;


    @PostMapping("/slack")
    public void testSlack(){
        var slackNotificationOptional = slackNotificationRepository.findById(1);

        if (slackNotificationOptional.isPresent()) {
            var slackNotification = slackNotificationOptional.get();
            var slackMsg = "테스트 메세지";
            var slackNotificationLog = slackNotificationLogRepository.save(
                new SlackNotificationLog(
                  null,
                  slackNotification,
                  "/slack",
                  "testSlack",
                   slackMsg,
                  "",
                  LocalDateTime.now()
                )
            );
            SlackNotificationUtil.send(slackMsg, slackNotification.getUrl(), slackNotificationLog);
        }
    }
}
