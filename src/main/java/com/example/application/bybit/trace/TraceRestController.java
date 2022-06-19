package com.example.application.bybit.trace;

import com.example.application.bybit.trace.dto.response.CheckResult;
import com.example.application.bybit.trace.dto.response.TraceExitSetResult;
import com.example.application.bybit.trace.dto.response.TraceTargetSetResult;
import com.example.application.bybit.trace.enums.CHECK_RESULT;
import com.example.application.bybit.trace.enums.EXIT_RESULT;
import com.example.application.bybit.trace.enums.TARGET_RESULT;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// [Socket Jar 주의점]
// traceTargetSet 거래 금액이 달라졌을시만 체크
// traceTargetSet 종료안된 시점에서 traceExitSet 실행하면 안됨
// traceExitSet 는 List Size 가 0일때까지 계속 실행해야함

@RestController
@RequiredArgsConstructor
@RequestMapping("/trace")
public class TraceRestController {
    private final TraceService traceService;

    /**
     * 최초 한번 실행 [Socket 초기 설정 용도]
     * @param minuteBong 봉
     * @return CheckResult
     */
    @GetMapping("/check")
    public CheckResult check (
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong
    ) {
        if (minuteBong.equals(0)) {
            return new CheckResult(CHECK_RESULT.NO_MINUTE_BONG);
        }
        return traceService.check(minuteBong);
    }

    /**
     * 진입 금액 세팅
     * [초기 세팅 값 [check]을 확인 후 봉 데이터 기반으로 진입 금액을 도달했을때 실행]
     * @param minuteBong 봉
     * @param price 진입 금액 (완전 현재가 아님)
     * @param isBuy 매수,매도 여부
     * @param basePrice 고점, 저점
     * @return TraceTargetSetResult
     */
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

    /**
     * 청산 금액 세팅
     * [진입 금액 세팅 또는 진입 금액 한계 지점까지 도달했을 경우 진입 금액 체크 확인] -> [청산 금액 세팅]
     * @param minuteBong 봉
     * @return TraceExitSetResult
     */
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
