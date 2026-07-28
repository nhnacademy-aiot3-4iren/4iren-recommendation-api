package com.nhnacademy.recommendation.config;

import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import org.springframework.stereotype.Component;

@Component
public class LlmRequestContextHolder {
    private static final ThreadLocal<LlmRequestContext> CONTEXT = new ThreadLocal<>();

    public void set(LlmRequestContext context){
        CONTEXT.set(context);
    }

    public LlmRequestContext get(){
        LlmRequestContext context = CONTEXT.get();
        if(context == null){
            throw new IllegalArgumentException("LLM 요청 컨텍스트가 없습니다.");
        }
        return context;
    }

    public void clear(){
        CONTEXT.remove();
    }
}
