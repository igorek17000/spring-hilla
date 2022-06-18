package com.example.application.bybit.trace;

import com.example.application.bybit.trace.dto.response.TraceExitSetResult;
import com.example.application.bybit.trace.dto.response.TraceTargetSetResult;
import com.example.application.bybit.trace.enums.EXIT_RESULT;
import com.example.application.bybit.trace.enums.TARGET_RESULT;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/trace")
public class TraceRestController {
    private final TraceService traceService;

    @GetMapping("/target/set")
    public TraceTargetSetResult traceTargetSet(
            @RequestParam(name = "minuteBong", defaultValue = "0")  Integer minuteBong,
            @RequestParam(name = "price", defaultValue = "0.0")     Double price,
            @RequestParam(name = "isBuy", defaultValue = "true")    boolean isBuy,
            @RequestParam(name = "basePrice", defaultValue = "0.0") Double basePrice
    ){
        if (minuteBong.equals(0)) {
            return new TraceTargetSetResult(TARGET_RESULT.NO_MEMBER);
        }

        if (price.equals(0.0)) {
            return new TraceTargetSetResult(TARGET_RESULT.NO_MINUTE_BONG);
        }

        return traceService.traceTargetSet(minuteBong, price, isBuy, basePrice);
    }

    @GetMapping("/exit/set")
    public TraceExitSetResult traceExitSet(
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong
    ){
        if (minuteBong.equals(0)) {
            return new TraceExitSetResult(EXIT_RESULT.NO_MEMBER);
        }

        return  traceService.traceExitSet(minuteBong);
    }

}
