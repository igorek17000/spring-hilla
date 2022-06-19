package com.example.application.bybit.trace.dto.response;

import com.example.application.bybit.trace.entity.Bong;
import com.example.application.bybit.trace.enums.CHECK_RESULT;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;


@Data
public class CheckResult {

    private Integer code;
    private String message;

    private List<Bong> bongList;

    public CheckResult() {
        code = 0;
        message = "정상적으로 조회가 완료되었습니다.";
        bongList = new ArrayList<>();
    }

    public CheckResult(CHECK_RESULT check_result){
        setResult(check_result);
    }

    public void setResult(CHECK_RESULT check_result){
        code = check_result.getCode();
        message = check_result.getMessage();
    }

}

