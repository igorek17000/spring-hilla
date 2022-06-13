package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


// TODO Mapping이 중복 실행 안되게 하는 방안?

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit/trace")
public class TraceRestController {
    private final TraceService traceService;

    @GetMapping("/{minuteBong}/{price}")
    public List<Trace> common_trace_set(
            @PathVariable Integer minuteBong,
            @PathVariable Double price,
            @RequestParam(name = "isBuy", defaultValue = "true") boolean isBuy,
            @RequestParam(name = "basePrice", defaultValue = "0.0") Double basePrice

    ){
        var traces = traceService.commonTraceSet(minuteBong, price, isBuy, basePrice);
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
