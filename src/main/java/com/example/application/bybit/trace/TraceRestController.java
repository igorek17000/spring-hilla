package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit/trace")
public class TraceRestController {
    private final TraceService traceService;

    // Jar 주의점 [중복 실행],
    // common_trace_set 종료안된시점에서 common_trace_start 실행하면 안됨
    @GetMapping("/{minuteBong}/{price}")
    public List<Trace> common_trace_set(
            @PathVariable Integer minuteBong,
            @PathVariable Double price){
        var traces = traceService.commonTraceSet(minuteBong, price);

        if (traces.size() == 0) {
            return null;
        }

        return traces;
    }

    @GetMapping("/{minuteBong}")
    public List<Trace> common_trace_start(
            @PathVariable Integer minuteBong
    ){
        var traces = traceService.commonTraceStart(minuteBong);

        if (traces.size() == 0) {
            return null;
        }
        return traces;
    }

    // TODO 수정해야함
    @GetMapping("/individual/{memberIdx}/{minuteBong}")
    public String individual_check(
            @PathVariable Integer memberIdx,
            @PathVariable Integer minuteBong
    ){

//        var optionalMemberApi = memberService.getApi(memberIdx, minuteBong);
//
//        if (optionalMemberApi.isPresent()) {
//
//            var memberApi = optionalMemberApi.get();
//
//            var client = new StandardWebSocketClient();
//            var handler = new TraceIndividualHandler(traceService, memberApi.getSecretKey(), memberApi.getApiKey(), memberIdx, minuteBong);
//            var connectionManager = new WebSocketConnectionManager(client, handler,"wss://stream.bybit.com/realtime");
//            connectionManager.start();
//
//            return "OK";
//        }
        return "FAIL";
    }

}
