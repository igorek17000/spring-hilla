package com.example.application.bybit.trace;

import com.example.application.bybit.trace.entity.Trace;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trace")
public class TraceRestController {
    private final TraceService traceService;

    @GetMapping("/target/set")
    public List<Trace> traceTargetSet(
            @RequestParam(name = "minuteBong", defaultValue = "0")  Integer minuteBong,
            @RequestParam(name = "price", defaultValue = "0.0")     Double price,
            @RequestParam(name = "isBuy", defaultValue = "true")    boolean isBuy,
            @RequestParam(name = "basePrice", defaultValue = "0.0") Double basePrice
    ){
        // TODO

//        if (minuteBong.equals(0)) {
//            return null;
//        }
//
//        if (price.equals(0.0)) {
//            return null;
//        }

        var traces = traceService.traceTargetSet(minuteBong, price, isBuy, basePrice);
        if (traces.size() == 0) {
            return null;
        }

        return traces;
    }

    @GetMapping("/exit/set")
    public List<Trace> traceExitSet(
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong
    ){
        if (minuteBong.equals(0)) {
            return null;
        }

        var traces = traceService.traceExitSet(minuteBong);
        if (traces.size() == 0) {
            return null;
        }

        return traces;
    }

}
