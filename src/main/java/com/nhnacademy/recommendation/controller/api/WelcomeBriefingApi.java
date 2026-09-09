package com.nhnacademy.recommendation.controller.api;

import com.nhnacademy.recommendation.dto.ErrorResponse;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingRequest;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Welcome Briefing", description = "강의실 웰컴 브리핑 API")
public interface WelcomeBriefingApi {

    @Operation(
            summary = "웰컴 브리핑 생성",
            description = "현재 센서 상태, 외부 날씨, 기기 목록, ML 추천 스케줄을 조합해 오늘의 강의실 관리 브리핑을 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "웰컴 브리핑 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류 또는 생성 가능 시간 초과",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    WelcomeBriefingResponse generateWelcomeBriefing(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "웰컴 브리핑 생성 요청",
                    required = true
            )
            WelcomeBriefingRequest request
    );

    @Operation(summary = "웰컴 브리핑 캐시 삭제", description = "저장된 웰컴 브리핑 캐시를 전체 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "캐시 삭제 성공")
    ResponseEntity<Void> clearWelcomeBriefingCache();
}
