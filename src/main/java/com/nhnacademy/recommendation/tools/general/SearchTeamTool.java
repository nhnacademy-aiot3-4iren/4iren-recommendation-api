package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.config.LlmRequestContextHolder;
import com.nhnacademy.recommendation.dto.PageResponse;
import com.nhnacademy.recommendation.dto.llm.LlmRequestContext;
import com.nhnacademy.recommendation.dto.team.TeamResponse;
import com.nhnacademy.recommendation.service.LlmConversationContextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchTeamTool {

    private final CoreClient coreClient;
    private final LlmRequestContextHolder llmRequestContextHolder;
    private final LlmConversationContextService llmConversationContextService;

    public List<TeamResponse> getTeamsByUser(){
        LlmRequestContext context = llmRequestContextHolder.get();
        PageResponse<TeamResponse> teamPage = coreClient.getTeamsByUser(context.userId(), context.role());
        return teamPage.content().stream().toList();

    }
}
