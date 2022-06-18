package com.example.application.bybit.trace.dto.response;

import com.example.application.bybit.trace.entity.Trace;
import com.example.application.bybit.trace.enums.EXIT_RESULT;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TraceExitSetResult {
    private Integer code;
    private String message;
    private List<Trace> traces;

    public TraceExitSetResult(){
        code = 0;
        message = "정상적으로 설정이 완료되었습니다.";
        traces = new ArrayList<>();
    }

    public TraceExitSetResult(EXIT_RESULT exit_result){
        setResult(exit_result);
        traces = new ArrayList<>();
    }

    public void setResult(EXIT_RESULT exit_result){
        code = exit_result.getCode();
        message = exit_result.getMessage();
    }

}

