package com.nhnacademy.recommendation.service.welcomebriefing;

import com.nhnacademy.recommendation.dto.welcomebriefing.WelcomeBriefingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WelcomeBriefingCacheServiceTest {

    @Mock
    WelcomeBriefingService welcomeBriefingService;

    WelcomeBriefingCacheService service;

    @BeforeEach
    void setUp() {
        service = new WelcomeBriefingCacheService(welcomeBriefingService);
    }

    @Test
    @DisplayName("웰컴 브리핑 생성을 실제 서비스에 위임한다")
    void generateWelcomeBriefing() {
        LocalDate date = LocalDate.of(2026, 8, 24);
        WelcomeBriefingResponse response = new WelcomeBriefingResponse(
                "요약",
                "현재 상태",
                "비교",
                List.of("추천"),
                List.of("확인")
        );
        given(welcomeBriefingService.generateWelcomeBriefing(3L, 10L)).willReturn(response);

        WelcomeBriefingResponse result = service.generateWelcomeBriefing(3L, 10L, date);

        assertThat(result).isEqualTo(response);
        verify(welcomeBriefingService).generateWelcomeBriefing(3L, 10L);
    }
}
