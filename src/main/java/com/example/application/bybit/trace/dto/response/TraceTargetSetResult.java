package com.example.application.bybit.trace.dto.response;

import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.enums.TARGET_RESULT;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceTargetSetResult {
    private Integer code;
    private String message;
    private List<Trace> traces;

    public TraceTargetSetResult(){
        code = 0;
        message = "정상 조회가 완료되었습니다.";
        traces = new ArrayList<>();
    }

    public TraceTargetSetResult(TARGET_RESULT target_result){
        setResult(target_result);
        traces = new ArrayList<>();
    }

    public void setResult(TARGET_RESULT target_result){
        code = target_result.getCode();
        message = target_result.getMessage();
    }

}

