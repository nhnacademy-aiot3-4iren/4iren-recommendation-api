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
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ActiveBehaviorRecommendationIntegrationTest {

    private static final Map<String, String> EXPECTED_BEHAVIOR_SHA256 = Map.of(
            "behavior_daily_usage_air_conditioner.onnx",
            "8079abd6b4570f53376e17110caba03ff6d8f4c09f487fe80be1709eb3e70a8d",
            "behavior_daily_usage_heater.onnx",
            "e0d2894e781cb14bd3fce0145f80841a7fb6ca3fe6b720640a8413e05d65a908",
            "behavior_event_air_conditioner_start.onnx",
            "e698ccab5f3a99d923d47d5628c0cf0e0f6591c2e6428ab6616aecb358889c5c",
            "behavior_event_air_conditioner_stop.onnx",
            "442a5e724160897c87b8f732863a8de7fc81ce13fa117306a06b4c8e33170e08",
            "behavior_event_heater_start.onnx",
            "4f2252f387088e735d36e1b0ffbbc686715803968d84199771992bb71e362e0b",
            "behavior_event_heater_stop.onnx",
            "a45717bc596b224f5ead8f173e06a3bc2de41bc234729e170f9f65f007d04cc6",
            "behavior_event_ventilation.onnx",
            "4d3cca7a1d4b60b9d9bce87bbb0ba5b1e06bb65869ff0ddaa96e4e919426542a"
    );

    private static final Map<Long, String> LOCATIONS = Map.of(
            1L, "실습실",
            2L, "사무실",
            3L, "회의실"
    );

    private static final List<LocalDate> DATES = List.of(
            LocalDate.of(2026, 8, 25),
            LocalDate.of(2026, 8, 26),
            LocalDate.of(2026, 8, 27),
            LocalDate.of(2026, 8, 28)
    );

    @Test
    @EnabledIfSystemProperty(named = "model.bundle.test-dir", matches = ".+")
    void loadsActiveBundleAndRunsActualRecommendationService() throws Exception {
        Path bundleDirectory = Path.of(System.getProperty("model.bundle.test-dir"))
                .toAbsolutePath().normalize();
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ModelBundleValidator validator = new ModelBundleValidator(objectMapper);
        ValidatedModelBundle bundle = validator.validate(bundleDirectory);
        MinioModelBundleDownloader downloader = new MinioModelBundleDownloader(null, null, null, null) {
            @Override
            public ValidatedModelBundle downloadAndValidate() {
                return bundle;
            }
        };

        try (OrtEnvironment environment = OrtEnvironment.getEnvironment("active-behavior-integration-test")) {
            ModelServingInfrastructure infrastructure = new ModelServingInfrastructure(
                    downloader,
                    objectMapper,
                    environment,
                    new OnnxSmokeTester(objectMapper, environment)
            );
            try {
                infrastructure.initialize();
                assertThat(infrastructure.bundle().directory()).isEqualTo(bundleDirectory);
                assertThat(infrastructure.sessions().size()).isEqualTo(10);
                verifyBehaviorHashes(bundleDirectory);

                BehaviorRecommendationService service = new BehaviorRecommendationService(
                        infrastructure, environment
                );
                Map<String, BehaviorRecommendationResult> results = new LinkedHashMap<>();
                for (LocalDate date : DATES) {
                    for (Map.Entry<Long, String> room : LOCATIONS.entrySet()) {
                        Map<String, Object> request = Map.of(
                                "predictionDate", date.toString(),
                                "roomId", room.getKey()
                        );
                        BehaviorRecommendationResult response = service.recommendWithDiagnostics(
                                date, room.getKey()
                        );
                        System.out.printf("RECOMMENDATION_SERVICE_CALL service=%s request=%s response=%s%n",
                                "BehaviorRecommendationService.recommendWithDiagnostics",
                                objectMapper.writeValueAsString(request),
                                objectMapper.writeValueAsString(response));
                        verifyResponse(response, date, room.getKey(), room.getValue());
                        results.put(date + "/" + room.getValue(), response);
                    }
                }
                verifyLabSanity(results);
                System.out.printf("ACTIVE_MODEL_PROOF modelVersion=%s path=%s hashes=%s%n",
                        infrastructure.bundle().manifest().modelVersion(),
                        infrastructure.bundle().directory(),
                        objectMapper.writeValueAsString(EXPECTED_BEHAVIOR_SHA256));
            } finally {
                infrastructure.close();
            }
        }
    }

    private void verifyBehaviorHashes(Path bundleDirectory) {
        EXPECTED_BEHAVIOR_SHA256.forEach((filename, expected) ->
                assertThat(ModelBundleValidator.sha256(bundleDirectory.resolve(filename)))
                        .as(filename)
                        .isEqualTo(expected));
    }

    private void verifyResponse(BehaviorRecommendationResult response,
                                LocalDate date,
                                Long roomId,
                                String location) {
        assertThat(response.temperatureRegime()).isEqualTo("COOLING");
        assertThat(response.recommendation().context().predictionDate()).isEqualTo(date);
        assertThat(response.recommendation().context().roomId()).isEqualTo(roomId);
        assertThat(response.recommendation().context().location()).isEqualTo(location);
        assertThat(response.dailyUsage().get("AIR_CONDITIONER").useToday()).isTrue();
        assertThat(response.dailyUsage().get("AIR_CONDITIONER").probability()).isFinite();

        HvacSchedule schedule = (HvacSchedule) response.eventSchedule().get("AIR_CONDITIONER");
        assertThat(schedule.sessions()).isNotEmpty();
        LocalTime previousStop = null;
        for (Session session : schedule.sessions()) {
            LocalTime start = LocalTime.parse(session.startTime());
            LocalTime stop = LocalTime.parse(session.stopTime());
            assertThat(start).isBefore(stop);
            assertThat(session.startProbability()).isFinite();
            assertThat(session.stopProbability()).isFinite().isGreaterThan(0.0);
            if (previousStop != null) {
                assertThat(start).isAfterOrEqualTo(previousStop);
            }
            previousStop = stop;
        }
    }

    private void verifyLabSanity(Map<String, BehaviorRecommendationResult> results) {
        Map<LocalDate, List<String>> expected = Map.of(
                LocalDate.of(2026, 8, 25), List.of("08:00", "23:00"),
                LocalDate.of(2026, 8, 26), List.of("08:30", "21:30"),
                LocalDate.of(2026, 8, 27), List.of("08:30", "22:30"),
                LocalDate.of(2026, 8, 28), List.of("08:30", "23:00")
        );
        expected.forEach((date, times) -> {
            HvacSchedule schedule = (HvacSchedule) results.get(date + "/실습실")
                    .eventSchedule().get("AIR_CONDITIONER");
            assertThat(schedule.sessions()).hasSize(1);
            assertThat(schedule.sessions().getFirst().startTime()).isEqualTo(times.get(0));
            assertThat(schedule.sessions().getFirst().stopTime()).isEqualTo(times.get(1));
        });
    }
}
