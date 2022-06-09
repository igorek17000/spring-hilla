package com.example.application.bybit.trace;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import com.example.application.member.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit/trace")
public class TraceRestController {

    private final TraceService traceService;
    private final MemberService memberService;

    @GetMapping("/{minuteBong}")
    public String common_trace(
            @PathVariable Integer minuteBong,
            @RequestParam(name = "price", defaultValue = "0.0") Double price
    ){

        if (price.equals(0.0)) {
            return "FAIL";
        }

        if (traceService.common_trace(minuteBong, price)){
            return "FAIL";
        }

        return "OK";
    }

    @GetMapping("/individual/{memberIdx}/{minuteBong}")
    public String individual_check(
            @PathVariable Integer memberIdx,
            @PathVariable Integer minuteBong
    ){

        var optionalMemberApi = memberService.getApi(memberIdx, minuteBong);

        if (optionalMemberApi.isPresent()) {

            var memberApi = optionalMemberApi.get();

            var client = new StandardWebSocketClient();
            var handler = new TraceIndividualHandler(traceService, memberApi.getSecretKey(), memberApi.getApiKey(), memberIdx, minuteBong);
            var connectionManager = new WebSocketConnectionManager(client, handler,"wss://stream.bybit.com/realtime");
            connectionManager.start();

            return "OK";
        }
        return "FAIL";
    }

}
