package com.example.application.bybit.trace;

import com.example.application.bybit.trace.enums.ORDER_TYPE;
import com.example.application.bybit.trace.enums.SIDE;
import com.example.application.bybit.trace.enums.TIME_IN_FORCE;
import com.example.application.bybit.util.BybitOrderUtil;
import com.example.application.member.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/bybit")
public class BybitApiController {
    private final MemberService memberService;


    @GetMapping("/myWallet")
    public ResponseEntity<?> myWallet(
            @RequestParam(name = "memberIdx", defaultValue = "0") Integer memberIdx,
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong
    ){
        if (memberAndBongIsNull(minuteBong, memberIdx)) return null;

        var memberApiOptional = memberService.getApi(memberIdx, minuteBong);
        if (memberApiOptional.isEmpty()) {
            return null;
        }
        var memberApi = memberApiOptional.get();

        return BybitOrderUtil.my_wallet(memberApi.getApiKey(), memberApi.getSecretKey());
    }

    @GetMapping("/publicOrderList")
    public ResponseEntity<?> publicOrderList(){
        return BybitOrderUtil.publicOrderList();
    }


    @GetMapping("/orderList")
    public ResponseEntity<?> orderList(
            @RequestParam(name = "memberIdx", defaultValue = "0") Integer memberIdx,
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong,
            @RequestParam(name = "orderStatus", defaultValue = "Filled") String orderStatus
    ){
        if (memberAndBongIsNull(minuteBong, memberIdx)) return null;

        var memberApiOptional = memberService.getApi(memberIdx, minuteBong);
        if (memberApiOptional.isEmpty()) {
            return null;
        }
        var memberApi = memberApiOptional.get();

        // OrderStatus ex) "Filled,New"
        return BybitOrderUtil.order_list(memberApi.getApiKey(), memberApi.getSecretKey(), orderStatus);
    }
    @GetMapping("/order")
    public ResponseEntity<?> order(
            @RequestParam(name = "memberIdx", defaultValue = "0") Integer memberIdx,
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong,
            @RequestParam(name = "qty", defaultValue = "0") Integer qty,
            @RequestParam(name = "isBuy", defaultValue = "true") boolean isBuy,
            @RequestParam(name = "price", defaultValue = "0.0") Double price
    ) {

        if (memberAndBongIsNull(minuteBong, memberIdx)) return null;

        var memberApiOptional = memberService.getApi(memberIdx, minuteBong);
        if (memberApiOptional.isEmpty()) {
            return null;
        }
        var memberApi = memberApiOptional.get();

        /* price 0.0 이면 시장가 */
        return BybitOrderUtil.order(
                memberApi.getApiKey(),
                memberApi.getSecretKey(),
                qty,
                isBuy ? SIDE.Buy : SIDE.Sell,
                price.equals(0.0) ? TIME_IN_FORCE.GoodTillCancel :TIME_IN_FORCE.PostOnly,
                price,
                price.equals(0.0) ? ORDER_TYPE.Market :ORDER_TYPE.Limit
        );
    }

    @GetMapping("/position")
    public ResponseEntity<?> position(
            @RequestParam(name = "memberIdx", defaultValue = "0") Integer memberIdx,
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong
    ){
        if (memberAndBongIsNull(minuteBong, memberIdx)) return null;

        var memberApiOptional = memberService.getApi(memberIdx, minuteBong);
        if (memberApiOptional.isEmpty()) {
            return null;
        }
        var memberApi = memberApiOptional.get();

        return BybitOrderUtil.position(memberApi.getApiKey(), memberApi.getSecretKey());
    }


    @GetMapping("/orderCancel")
    public ResponseEntity<?> orderCancel(
            @RequestParam(name = "memberIdx", defaultValue = "0") Integer memberIdx,
            @RequestParam(name = "minuteBong", defaultValue = "0") Integer minuteBong,
            @RequestParam(name = "order_id", defaultValue = "") String order_id
    ){
        if (memberAndBongIsNull(minuteBong, memberIdx)) return null;

        var memberApiOptional = memberService.getApi(memberIdx, minuteBong);
        if (memberApiOptional.isEmpty()) {
            return null;
        }
        var memberApi = memberApiOptional.get();

        if (order_id.equals("")) {
            return null;
        }
        return BybitOrderUtil.order_cancel(memberApi.getApiKey(), memberApi.getSecretKey(), order_id);
    }

    private boolean memberAndBongIsNull(Integer minuteBong, Integer memberIdx) {
        if (minuteBong.equals(0)) {
            return true;
        }
        return memberIdx.equals(0);
    }




}
