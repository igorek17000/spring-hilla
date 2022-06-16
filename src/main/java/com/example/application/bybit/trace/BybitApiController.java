package com.example.application.bybit.trace;

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

    // order
    // order_cancel
    // order_list

    private boolean memberAndBongIsNull(Integer minuteBong, Integer memberIdx) {
        if (minuteBong.equals(0)) {
            return true;
        }
        return memberIdx.equals(0);
    }




}
