package com.nhnacademy.recommendation.service.welcomebriefing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.controller.WelcomeBriefingController;
import com.nhnacademy.recommendation.service.behavior.BehaviorRecommendationService;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class WelcomeBriefingDisabledContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withPropertyValues("model.serving.enabled=false")
            .withBean("welcomeBriefingChatClient", ChatClient.class, () -> mock(ChatClient.class))
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(CoreWeatherService.class, () -> mock(CoreWeatherService.class))
            .withBean(CoreRoomService.class, () -> mock(CoreRoomService.class))
            .withBean(WelcomeBriefingPolicyService.class, () -> mock(WelcomeBriefingPolicyService.class))
            .withUserConfiguration(WelcomeBriefingGraphConfiguration.class);

    @Test
    @DisplayName("Model serving이 비활성화되어도 Welcome Briefing bean graph가 시작된다")
    void startsWithoutBehaviorRecommendationService() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(WelcomeBriefingController.class);
            assertThat(context).hasSingleBean(WelcomeBriefingService.class);
            assertThat(context).doesNotHaveBean(BehaviorRecommendationService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            WelcomeBriefingController.class,
            WelcomeBriefingService.class,
            BehaviorRecommendationService.class
    })
    static class WelcomeBriefingGraphConfiguration {
    }
}
