package com.nhnacademy.recommendation.service.behavior;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendation;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.HvacSchedule;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.VentilationSchedule;
import com.nhnacademy.recommendation.model.serving.MinioModelBundleDownloader;
import com.nhnacademy.recommendation.model.serving.ModelBundleValidator;
import com.nhnacademy.recommendation.model.serving.ModelServingInfrastructure;
import com.nhnacademy.recommendation.model.serving.OnnxSmokeTester;
import com.nhnacademy.recommendation.model.serving.ValidatedModelBundle;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
class BehaviorRecommendationParityIntegrationTest {

    @Test
    void matchesAllBehaviorRequestsFromBundleParityFixture() throws Exception {
        Path bundleDirectory = bundleDirectory();
        assumeTrue(Files.isRegularFile(bundleDirectory.resolve("manifest.json")),
                "실제 v1 Bundle 경로가 없어 Behavior parity test를 건너뜁니다: " + bundleDirectory);

        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ValidatedModelBundle bundle = new ModelBundleValidator(objectMapper).validate(bundleDirectory);
        MinioModelBundleDownloader downloader = new MinioModelBundleDownloader(null, null, null, null) {
            @Override
            public ValidatedModelBundle downloadAndValidate() {
                return bundle;
            }
        };

        try (OrtEnvironment environment = OrtEnvironment.getEnvironment("behavior-parity-integration-test")) {
            ModelServingInfrastructure infrastructure = new ModelServingInfrastructure(
                    downloader,
                    objectMapper,
                    environment,
                    new OnnxSmokeTester(objectMapper, environment)
            );
            try {
                infrastructure.initialize();
                BehaviorRecommendationService service = new BehaviorRecommendationService(infrastructure, environment);
                JsonNode fixture = objectMapper.readTree(
                        bundle.directory().resolve(bundle.manifest().parityFixtureFilename()).toFile()
                );
                JsonNode behaviorFixture = fixture.path("behavior");
                boolean legacyFixture = behaviorFixture.path("requests").isArray();
                double relativeTolerance = legacyFixture
                        ? fixture.path("tolerances").path("rtol").doubleValue()
                        : behaviorFixture.path("rtol").doubleValue();
                double absoluteTolerance = legacyFixture
                        ? fixture.path("tolerances").path("atol").doubleValue()
                        : behaviorFixture.path("atol").doubleValue();
                JsonNode requests = legacyFixture
                        ? behaviorFixture.path("requests")
                        : behaviorFixture.path("serviceRequests");
                assertThat(requests).hasSize(4);

                for (JsonNode parityCase : requests) {
                    JsonNode request = legacyFixture ? parityCase.path("request") : parityCase;
                    LocalDate predictionDate = LocalDate.parse(request.path("predictionDate").asText());
                    Long roomId = request.path("roomId").longValue();

                    BehaviorRecommendationResult actual = service.recommendWithDiagnostics(predictionDate, roomId);
                    assertContext(actual.recommendation(),
                            legacyFixture ? parityCase.path("expectedResponse") : null, request);
                    assertThat(actual.temperatureRegime())
                            .isEqualTo(parityCase.path(legacyFixture
                                    ? "expectedTemperatureRegime" : "temperatureRegime").asText());
                    assertDailyUsage(actual, parityCase.path(legacyFixture
                                    ? "expectedDailyUsage" : "dailyUsage"), relativeTolerance,
                            absoluteTolerance);
                    assertEventSchedule(actual, parityCase.path(legacyFixture
                                    ? "expectedEventSchedule" : "eventSchedule"), relativeTolerance,
                            absoluteTolerance);
                    assertSchedule(actual.recommendation(), legacyFixture
                                    ? parityCase.path("expectedResponse").path("recommendedSchedule")
                                    : parityCase.path("recommendedSchedule"), relativeTolerance,
                            absoluteTolerance);

                    System.out.printf("Behavior parity PASS: predictionDate=%s roomId=%d location=%s%n",
                            predictionDate, roomId, actual.recommendation().context().location());
                }
            } finally {
                infrastructure.close();
            }
        }
    }

    private void assertContext(BehaviorRecommendation actual, JsonNode expectedResponse, JsonNode request) {
        if (expectedResponse != null) {
            JsonNode expected = expectedResponse.path("context");
            assertThat(actual.schemaVersion()).isEqualTo(expectedResponse.path("schemaVersion").asText());
            assertThat(actual.recommendationType()).isEqualTo(expectedResponse.path("recommendationType").asText());
            assertThat(actual.context().predictionDate().toString())
                    .isEqualTo(expected.path("predictionDate").asText());
            assertThat(actual.context().weekday().name()).isEqualTo(expected.path("weekday").asText());
            assertThat(actual.context().roomId()).isEqualTo(expected.path("roomId").longValue());
            assertThat(actual.context().location()).isEqualTo(expected.path("location").asText());
            assertThat(actual.context().timezone()).isEqualTo(expected.path("timezone").asText());
        }
        assertThat(actual.context().predictionDate().toString())
                .isEqualTo(request.path("predictionDate").asText());
        assertThat(actual.context().roomId()).isEqualTo(request.path("roomId").longValue());
        assertThat(actual.context().location()).isEqualTo(request.path("location").asText());
    }

    private void assertDailyUsage(BehaviorRecommendationResult actual,
                                  JsonNode expected,
                                  double rtol,
                                  double atol) {
        expected.fields().forEachRemaining(entry -> {
            BehaviorRecommendationResult.DeviceUsageDecision decision = actual.dailyUsage().get(entry.getKey());
            JsonNode value = entry.getValue();
            assertThat(decision).isNotNull();
            assertThat(decision.useToday()).isEqualTo(value.path("useToday").booleanValue());
            assertClose(decision.probability(), value.path("probability").doubleValue(), rtol, atol);
            assertClose(decision.threshold(), value.path("threshold").doubleValue(), rtol, atol);
            assertThat(decision.allowedByRegime()).isEqualTo(value.path("allowedByRegime").booleanValue());
        });
    }

    private void assertEventSchedule(BehaviorRecommendationResult actual,
                                     JsonNode expected,
                                     double rtol,
                                     double atol) {
        for (String deviceType : new String[]{"AIR_CONDITIONER", "HEATER"}) {
            HvacSchedule schedule = (HvacSchedule) actual.eventSchedule().get(deviceType);
            JsonNode value = expected.path(deviceType);
            assertThat(schedule.useToday()).isEqualTo(value.path("useToday").booleanValue());
            assertThat(schedule.recommendedSessions()).isEqualTo(value.path("recommendedSessions").intValue());
            assertClose(schedule.usageProbability(), value.path("usageProbability").doubleValue(), rtol, atol);
            assertClose(schedule.usageThreshold(), value.path("usageThreshold").doubleValue(), rtol, atol);
            if (value.has("sessionProfile")) {
                JsonNode profile = value.path("sessionProfile");
                assertThat(schedule.sessionProfile().availableDays()).isEqualTo(profile.path("availableDays").intValue());
                assertThat(schedule.sessionProfile().activeDays()).isEqualTo(profile.path("activeDays").intValue());
                assertClose(schedule.sessionProfile().meanEventsOnActiveDays(),
                        profile.path("meanEventsOnActiveDays").doubleValue(), rtol, atol);
                assertThat(schedule.sessionProfile().recommendedSessions())
                        .isEqualTo(profile.path("recommendedSessions").intValue());
                assertThat(schedule.sessionProfile().usedSameWeekday())
                        .isEqualTo(profile.path("usedSameWeekday").booleanValue());
                assertThat(schedule.sessions()).hasSize(value.path("sessions").size());
                for (int index = 0; index < schedule.sessions().size(); index++) {
                    JsonNode expectedSession = value.path("sessions").path(index);
                    BehaviorRecommendationResult.Session session = schedule.sessions().get(index);
                    assertThat(session.startTime()).isEqualTo(expectedSession.path("startTime").asText());
                    assertThat(session.stopTime()).isEqualTo(expectedSession.path("stopTime").asText());
                    assertClose(session.startProbability(), expectedSession.path("startProbability").doubleValue(),
                            rtol, atol);
                    assertClose(session.stopProbability(), expectedSession.path("stopProbability").doubleValue(),
                            rtol, atol);
                }
            }
        }

        VentilationSchedule ventilation = (VentilationSchedule) actual.eventSchedule().get("VENTILATION");
        JsonNode expectedVentilation = expected.path("VENTILATION");
        assertThat(ventilation.recommendedEvents())
                .isEqualTo(expectedVentilation.path("recommendedEvents").intValue());
        if (expectedVentilation.has("meanEventsPerDay")) {
            assertClose(ventilation.meanEventsPerDay(), expectedVentilation.path("meanEventsPerDay").doubleValue(),
                    rtol, atol);
            assertThat(ventilation.usedSameWeekday())
                    .isEqualTo(expectedVentilation.path("usedSameWeekday").booleanValue());
        }
        if (expectedVentilation.has("events")) {
            assertThat(ventilation.events()).hasSize(expectedVentilation.path("events").size());
            for (int index = 0; index < ventilation.events().size(); index++) {
                JsonNode expectedEvent = expectedVentilation.path("events").path(index);
                BehaviorRecommendationResult.VentilationEvent event = ventilation.events().get(index);
                assertThat(event.startTime()).isEqualTo(expectedEvent.path("startTime").asText());
                assertClose(event.probability(), expectedEvent.path("probability").doubleValue(), rtol, atol);
            }
        }
    }

    private void assertSchedule(BehaviorRecommendation actual,
                                JsonNode expected,
                                double rtol,
                                double atol) {
        assertThat(actual.recommendedSchedule()).hasSize(expected.size());
        for (int index = 0; index < actual.recommendedSchedule().size(); index++) {
            BehaviorRecommendation.ScheduleItem item = actual.recommendedSchedule().get(index);
            JsonNode value = expected.path(index);
            assertThat(item.deviceType()).isEqualTo(value.path("deviceType").asText());
            assertThat(item.action()).isEqualTo(value.path("action").asText());
            assertThat(item.startTime().toString()).isEqualTo(value.path("startTime").asText());
            if (value.has("endTime")) {
                assertThat(item.endTime().toString()).isEqualTo(value.path("endTime").asText());
            } else {
                assertThat(item.endTime()).isNull();
            }
            assertClose(item.confidence(), value.path("confidence").doubleValue(), rtol, atol);
        }
    }

    private void assertClose(double actual, double expected, double rtol, double atol) {
        assertThat(Math.abs(actual - expected)).isLessThanOrEqualTo(atol + rtol * Math.abs(expected));
    }

    private Path bundleDirectory() {
        String configured = System.getProperty("model.bundle.test-dir");
        return configured == null || configured.isBlank()
                ? Path.of("../4iren-machine-learning/artifacts/versions/v1").toAbsolutePath().normalize()
                : Path.of(configured).toAbsolutePath().normalize();
    }
}
