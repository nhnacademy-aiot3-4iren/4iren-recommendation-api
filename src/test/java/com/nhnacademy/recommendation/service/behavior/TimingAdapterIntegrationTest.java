package com.nhnacademy.recommendation.service.behavior;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.HvacSchedule;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.Session;
import com.nhnacademy.recommendation.model.serving.MinioModelBundleDownloader;
import com.nhnacademy.recommendation.model.serving.ModelBundleValidator;
import com.nhnacademy.recommendation.model.serving.ModelServingInfrastructure;
import com.nhnacademy.recommendation.model.serving.OnnxSmokeTester;
import com.nhnacademy.recommendation.model.serving.ValidatedModelBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class TimingAdapterIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "model.bundle.timing-test-dir", matches = ".+")
    void appliesOptionalActualActionTimingPriorToRealOnnxScores() throws Exception {
        Path bundleDirectory = Path.of(System.getProperty("model.bundle.timing-test-dir"));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ValidatedModelBundle bundle = new ModelBundleValidator(objectMapper).validate(bundleDirectory);
        MinioModelBundleDownloader downloader = new MinioModelBundleDownloader(null, null, null, null) {
            @Override
            public ValidatedModelBundle downloadAndValidate() {
                return bundle;
            }
        };

        try (OrtEnvironment environment = OrtEnvironment.getEnvironment("behavior-timing-adapter-test")) {
            ModelServingInfrastructure infrastructure = new ModelServingInfrastructure(
                    downloader, objectMapper, environment, new OnnxSmokeTester(objectMapper, environment)
            );
            try {
                infrastructure.initialize();
                assertThat(infrastructure.runtimeArtifacts().optionalCsv("behaviorActionTimePrior"))
                        .isPresent();
                BehaviorRecommendationResult result = new BehaviorRecommendationService(
                        infrastructure, environment
                ).recommendWithDiagnostics(LocalDate.of(2026, 9, 4), 1L);
                HvacSchedule schedule = (HvacSchedule) result.eventSchedule().get("AIR_CONDITIONER");
                Session session = schedule.sessions().getFirst();

                assertThat(session.startTime()).isEqualTo("08:30");
                assertThat(session.stopTime()).isEqualTo("19:30");
                assertThat(session.startProbability()).isGreaterThan(0.78);
                assertThat(session.stopProbability()).isGreaterThan(0.48);
                assertThat(result.recommendation().recommendedSchedule().stream()
                        .filter(item -> item.deviceType().equals("AIR_CONDITIONER")
                                && item.action().equals("ON"))
                        .findFirst().orElseThrow().confidence()).isGreaterThan(0.63);
            } finally {
                infrastructure.close();
            }
        }
    }
}
