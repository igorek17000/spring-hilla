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
    public List<Trace> traceTargetSet(
            @PathVariable Integer minuteBong,
            @PathVariable Double price,
            @RequestParam(name = "isBuy", defaultValue = "true") boolean isBuy,
            @RequestParam(name = "basePrice", defaultValue = "0.0") Double basePrice

    ){
        var traces = traceService.traceTargetSet(minuteBong, price, isBuy, basePrice);
        if (traces.size() == 0) {
            return null;
        }
        return traces;
    }

    @GetMapping("/{minuteBong}")
    public List<Trace> traceExitSet(
            @PathVariable Integer minuteBong
    ){
        var traces = traceService.traceExitSet(minuteBong);
        if (traces.size() == 0) {
            return null;
        }
        return traces;
    }
}
