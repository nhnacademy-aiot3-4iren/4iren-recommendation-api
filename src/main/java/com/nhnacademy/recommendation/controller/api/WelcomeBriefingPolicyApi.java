package com.nhnacademy.recommendation.controller.api;

import com.nhnacademy.recommendation.dto.ErrorResponse;
import com.nhnacademy.recommendation.dto.UserRole;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyDto;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyEnabledRequest;
import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingPolicyResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Welcome Briefing Policy", description = "웰컴 브리핑 날씨 판단 정책 API")
public interface WelcomeBriefingPolicyApi {

    @Operation(summary = "브리핑 정책 조회", description = "팀과 강의실 기준 웰컴 브리핑 정책을 조회합니다. 없으면 기본 정책을 반환합니다.")
    @ApiResponse(responseCode = "200", description = "정책 조회 성공")
    WelcomeBriefingPolicyDto getPolicy(
            @Parameter(description = "팀 ID", example = "3") Long teamId,
            @Parameter(description = "강의실 ID", example = "10") Long roomId
    );

    @Operation(summary = "브리핑 정책 생성", description = "팀과 강의실 기준 웰컴 브리핑 정책을 생성합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "정책 생성 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "정책 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 정책", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    WelcomeBriefingPolicyResponse createPolicy(
            @Parameter(description = "요청 사용자 ID", required = true, example = "1") Long userId,
            @Parameter(description = "요청 사용자 역할", required = true, example = "ADMIN") UserRole userRole,
            @Parameter(description = "팀 ID", example = "3") Long teamId,
            @Parameter(description = "강의실 ID", example = "10") Long roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "생성할 브리핑 정책", required = true)
            WelcomeBriefingPolicyDto request
    );

    @Operation(summary = "브리핑 정책 수정", description = "팀과 강의실 기준 웰컴 브리핑 정책 값을 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "정책 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "정책 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "정책 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    WelcomeBriefingPolicyResponse updatePolicy(
            @Parameter(description = "요청 사용자 ID", required = true, example = "1") Long userId,
            @Parameter(description = "요청 사용자 역할", required = true, example = "ADMIN") UserRole userRole,
            @Parameter(description = "팀 ID", example = "3") Long teamId,
            @Parameter(description = "강의실 ID", example = "10") Long roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "수정할 브리핑 정책", required = true)
            WelcomeBriefingPolicyDto request
    );

    @Operation(summary = "브리핑 정책 활성화 상태 수정", description = "웰컴 브리핑 정책의 활성화 여부만 수정합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "활성화 상태 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "정책 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "정책 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    void updatePolicyEnabled(
            @Parameter(description = "요청 사용자 ID", required = true, example = "1") Long userId,
            @Parameter(description = "요청 사용자 역할", required = true, example = "ADMIN") UserRole userRole,
            @Parameter(description = "팀 ID", example = "3") Long teamId,
            @Parameter(description = "강의실 ID", example = "10") Long roomId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "활성화 상태 요청", required = true)
            WelcomeBriefingPolicyEnabledRequest request
    );

    @Operation(summary = "브리핑 정책 삭제", description = "팀과 강의실 기준 웰컴 브리핑 정책을 삭제합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "정책 삭제 성공"),
            @ApiResponse(responseCode = "400", description = "요청 값 오류", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "정책 변경 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "정책 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    void deletePolicy(
            @Parameter(description = "요청 사용자 ID", required = true, example = "1") Long userId,
            @Parameter(description = "요청 사용자 역할", required = true, example = "ADMIN") UserRole userRole,
            @Parameter(description = "팀 ID", example = "3") Long teamId,
            @Parameter(description = "강의실 ID", example = "10") Long roomId
    );
}
