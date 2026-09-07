package com.nhnacademy.recommendation.controller.api;

import com.nhnacademy.recommendation.dto.ErrorResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.llm.LlmRequestDto;
import com.nhnacademy.recommendation.dto.llm.LlmResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "LLM Chat", description = "도구 호출 기반 LLM 챗봇 API")
public interface LlmApi {

    @Operation(
            summary = "챗봇 답변 생성",
            description = "사용자 질문과 구독 강의실 정보를 바탕으로 필요한 도구를 호출하고 최종 답변을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "답변 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 메시지 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    LlmResponseDto getChatAnswer(
            @Parameter(description = "요청 사용자 ID", required = true, example = "1")
            Long userId,
            @Parameter(description = "요청 사용자 역할", required = true, example = "NORMAL")
            UserRole role,
            @Parameter(description = "클라이언트 타입. WEB 또는 TELEGRAM", example = "WEB")
            String clientType,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "챗봇 질문 요청",
                    required = true
            )
            LlmRequestDto request
    );
}
