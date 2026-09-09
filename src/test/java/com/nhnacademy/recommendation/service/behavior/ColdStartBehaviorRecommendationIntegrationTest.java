package com.nhnacademy.recommendation.service.behavior;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult;
import com.nhnacademy.recommendation.model.serving.MinioModelBundleDownloader;
import com.nhnacademy.recommendation.model.serving.ModelBundleValidator;
import com.nhnacademy.recommendation.model.serving.ModelServingInfrastructure;
import com.nhnacademy.recommendation.model.serving.OnnxSmokeTester;
import com.nhnacademy.recommendation.model.serving.ValidatedModelBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ColdStartBehaviorRecommendationIntegrationTest {

    private static final Map<Long, String> EXISTING_PROFILE_LOCATIONS = Map.of(
            1L, "실습실",
            2L, "사무실",
            3L, "회의실"
    );

    @Test
    @EnabledIfSystemProperty(named = "model.bundle.test-dir", matches = ".+")
    void servesExistingAndColdStartRooms() throws Exception {
        Path bundleDirectory = Path.of(System.getProperty("model.bundle.test-dir"))
                .toAbsolutePath()
                .normalize();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ValidatedModelBundle bundle = new ModelBundleValidator(objectMapper).validate(bundleDirectory);
        MinioModelBundleDownloader downloader = new MinioModelBundleDownloader(null, null, null, null) {
            @Override
            public ValidatedModelBundle downloadAndValidate() {
                return bundle;
            }
        };

        try (OrtEnvironment environment = OrtEnvironment.getEnvironment("cold-start-behavior-test")) {
            ModelServingInfrastructure infrastructure = new ModelServingInfrastructure(
                    downloader,
                    objectMapper,
                    environment,
                    new OnnxSmokeTester(objectMapper, environment)
            );
            try {
                infrastructure.initialize();
                BehaviorRecommendationService service = new BehaviorRecommendationService(
                        infrastructure,
                        environment
                );
                for (long roomId : List.of(1L, 2L, 3L, 6L, 7L, 8L, 100L)) {
                    BehaviorRecommendationResult result = service.recommendWithDiagnostics(
                            LocalDate.of(2026, 8, 25),
                            roomId,
                            "core-room-" + roomId
                    );

                    assertThat(result.recommendation().context().roomId()).isEqualTo(roomId);
                    if (EXISTING_PROFILE_LOCATIONS.containsKey(roomId)) {
                        assertThat(result.recommendation().context().location())
                                .isEqualTo(EXISTING_PROFILE_LOCATIONS.get(roomId));
                    } else {
                        assertThat(result.recommendation().context().location())
                                .isEqualTo("core-room-" + roomId);
                    }
                    assertThat(result.dailyUsage().values())
                            .allSatisfy(decision -> assertThat(decision.probability()).isFinite());
                }
            } finally {
                infrastructure.close();
            }
        }
    }
}
