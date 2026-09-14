package com.nhnacademy.recommendation.controller.api;

import com.nhnacademy.recommendation.dto.ErrorResponse;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryRequest;
import com.nhnacademy.recommendation.dto.dailysummary.DailySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Daily Summary", description = "강의실 하루 환경 요약 API")
public interface DailySummaryApi {

    @Operation(
            summary = "하루 요약 생성",
            description = "강의실의 센서 시계열과 외부 날씨 히스토리를 바탕으로 하루 요약 리포트를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "하루 요약 생성 성공"),
            @ApiResponse(
                    responseCode = "400",
                    description = "요청 값 오류",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    DailySummaryResponse generateDailySummary(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "하루 요약 생성 요청. date, startHour, endHour를 생략하면 서비스 기본값을 사용합니다.",
                    required = true
            )
            DailySummaryRequest request
    );

    @Operation(summary = "하루 요약 캐시 삭제", description = "저장된 하루 요약 캐시를 전체 삭제합니다.")
    @ApiResponse(responseCode = "204", description = "캐시 삭제 성공")
    ResponseEntity<Void> clearDailySummaryCache();
}
