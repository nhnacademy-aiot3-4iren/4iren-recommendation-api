package com.nhnacademy.recommendation.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatClientConfigTest {

    @Test
    @DisplayName("웰컴 브리핑 프롬프트는 낮은 신뢰도에서도 추천 일정을 보존한다")
    void welcomeBriefingSystemPrompt_PreservesScheduleForLowConfidenceRecommendation() {
        assertThat(ChatClientConfig.WELCOME_BRIEFING_SYSTEM_PROMPT)
                .contains("모든 deviceType(AIR_CONDITIONER, HEATER, VENTILATION 등을 포함)")
                .contains("startTime 또는 endTime이 있으면, 해당 추천 조치에 제공된 시간을 반드시 명시하세요.")
                .contains("confidence가 낮더라도 원래 startTime/endTime을 제거하거나")
                .contains("\"가동 유지\", \"적절한 시간\" 같은 모호한 표현으로 대체하지 마세요.")
                .contains("confidence가 낮은 경우에도 추천 시간은 그대로 유지하고")
                .contains("(확인 필요)")
                .contains("confidence 수치나 수준은 사용자 응답에 직접 표시하지 마세요.")
                .doesNotContain("신뢰도 낮음")
                .contains("현재 시각이 startTime 이후여도 endTime이 남아 있으면")
                .contains("08:30~23:00")
                .contains("mlRecommendation에 없는 시간은 절대 생성하지 마세요.");
    }
}
