package com.example.application.bybit;

import com.example.application.bybit.trace.TraceHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.client.WebSocketConnectionManager;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit")
public class BybitSocketRestController {

    private final BybitService bybitService;

    @GetMapping("/trace/{idx}/{bong}")
    public void connect(
            @RequestParam(name = "secretKey", defaultValue = "") String secretKey,
            @RequestParam(name = "apiKey",    defaultValue = "") String apiKey,
            @PathVariable Integer idx,
            @PathVariable Integer bong){
        var client = new StandardWebSocketClient();
        var handler = new TraceHandler(bybitService, secretKey, apiKey, idx, bong);
        var connectionManager = new WebSocketConnectionManager(client, handler,"wss://stream.bybit.com/realtime");
        connectionManager.start();
    }




}
