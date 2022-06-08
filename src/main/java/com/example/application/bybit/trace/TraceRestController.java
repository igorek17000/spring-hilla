package com.example.application.bybit.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.example.application.member.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit")
public class TraceRestController {

    private final TraceService traceService;
    private final MemberService memberService;

    @GetMapping("/trace/{memberIdx}/{minuteBong}")
    public String connect(
            @PathVariable Integer memberIdx,
            @PathVariable Integer minuteBong
    ){

        var optionalMemberApi = memberService.getApi(memberIdx, minuteBong);

        if (optionalMemberApi.isPresent()) {

            var memberApi = optionalMemberApi.get();

            var client = new StandardWebSocketClient();
            var handler = new TraceHandler(traceService, memberApi.getSecretKey(), memberApi.getApiKey(), memberIdx, minuteBong);
            var connectionManager = new WebSocketConnectionManager(client, handler,"wss://stream.bybit.com/realtime");
            connectionManager.start();

            return "OK";
        }
        return "FAIL";
    }




}
