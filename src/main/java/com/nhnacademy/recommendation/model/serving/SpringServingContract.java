package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.BundleValidationException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public record SpringServingContract(
        String schemaVersion,
        String modelVersion,
        String roomPreferenceFilename,
        Set<String> validBehaviorLocations,
        int behaviorBinMinutes,
        List<String> behaviorEventFeatureOrder,
        BehaviorOrchestrationSpec behaviorOrchestration,
        Map<String, OnnxModelSpec> models
) {

    private static final int EXPECTED_ONNX_MODEL_COUNT = 10;
    private static final Pattern FEATURE_CYCLE_PATTERN = Pattern.compile("/([0-9]+(?:\\.[0-9]+)?)\\)$");

    public static SpringServingContract load(Path contractPath, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(contractPath.toFile());
            String schemaVersion = requiredText(root, "schemaVersion", "contract");
            String modelVersion = requiredText(root, "modelVersion", "contract");
            String roomPreferenceFilename = requiredText(root.path("roomPreference"), "filename", "roomPreference");

            JsonNode behavior = root.path("behavior");
            Set<String> validLocations = new LinkedHashSet<>(
                    requiredTextArray(behavior.path("validLocations"), "behavior.validLocations")
            );
            if (validLocations.size() != behavior.path("validLocations").size()) {
                throw new BundleValidationException("behavior.validLocations에 중복 값이 있습니다.");
            }
            int binMinutes = behavior.path("binMinutes").asInt(-1);
            if (binMinutes <= 0 || 1440 % binMinutes != 0) {
                throw new BundleValidationException("behavior.binMinutes가 하루를 균등하게 나눌 수 없습니다: " + binMinutes);
            }
            List<String> eventFeatureOrder = requiredTextArray(
                    behavior.path("eventFeatureOrder"), "behavior.eventFeatureOrder"
            );
            BehaviorOrchestrationSpec behaviorOrchestration = loadBehaviorOrchestration(behavior);

            Map<String, OnnxModelSpec> models = new LinkedHashMap<>();
            addModel(models, "objective", root.path("objective"));
            addModel(models, "temperature", root.path("temperature"));
            addModel(models, "humidity", root.path("humidity"));
            addModels(models, "behavior.dailyUsage.", behavior.path("dailyUsage"));
            addModels(models, "behavior.events.", behavior.path("events"));

            if (models.size() != EXPECTED_ONNX_MODEL_COUNT) {
                throw new BundleValidationException(
                        "spring_serving_contract.json의 ONNX 모델 수가 10개가 아닙니다: " + models.size()
                );
            }
            long distinctFilenames = models.values().stream().map(OnnxModelSpec::filename).distinct().count();
            if (distinctFilenames != EXPECTED_ONNX_MODEL_COUNT) {
                throw new BundleValidationException("spring_serving_contract.json에 중복 ONNX filename이 있습니다.");
            }

            return new SpringServingContract(
                    schemaVersion,
                    modelVersion,
                    roomPreferenceFilename,
                    Set.copyOf(validLocations),
                    binMinutes,
                    List.copyOf(eventFeatureOrder),
                    behaviorOrchestration,
                    Map.copyOf(models)
            );
        } catch (IOException e) {
            throw new BundleValidationException("spring_serving_contract.json을 읽을 수 없습니다: " + contractPath, e);
        }
    }

    private static void addModels(Map<String, OnnxModelSpec> models, String prefix, JsonNode group) {
        if (!group.isObject()) {
            throw new BundleValidationException("contract 모델 그룹이 객체가 아닙니다: " + prefix);
        }
        group.fields().forEachRemaining(entry -> addModel(models, prefix + entry.getKey(), entry.getValue()));
    }

    private static void addModel(Map<String, OnnxModelSpec> models, String key, JsonNode node) {
        String filename = requiredText(node, "filename", key);
        List<OnnxInputSpec> inputs = new ArrayList<>();
        JsonNode inputNode = node.path("inputs");
        if (!inputNode.isArray() || inputNode.isEmpty()) {
            throw new BundleValidationException("contract 모델 input이 없습니다: " + key);
        }
        for (JsonNode input : inputNode) {
            String name = requiredText(input, "name", key + ".inputs");
            String dtype = requiredText(input, "dtype", key + ".inputs." + name);
            inputs.add(new OnnxInputSpec(name, dtype));
        }

        Set<String> outputs = new LinkedHashSet<>();
        JsonNode outputNode = node.path("outputs");
        if (!outputNode.isArray() || outputNode.isEmpty()) {
            throw new BundleValidationException("contract 모델 output이 없습니다: " + key);
        }
        for (JsonNode output : outputNode) {
            outputs.add(requiredText(output, "name", key + ".outputs"));
        }
        if (outputs.size() != outputNode.size()) {
            throw new BundleValidationException("contract 모델 output name이 중복됩니다: " + key);
        }

        OnnxModelSpec previous = models.put(
                key,
                new OnnxModelSpec(
                        key,
                        filename,
                        List.copyOf(inputs),
                        Set.copyOf(outputs),
                        optionalText(node, "outputName"),
                        optionalInteger(node, "positiveProbabilityColumn"),
                        optionalDouble(node, "threshold")
                )
        );
        if (previous != null) {
            throw new BundleValidationException("contract 모델 key가 중복됩니다: " + key);
        }
    }

    private static BehaviorOrchestrationSpec loadBehaviorOrchestration(JsonNode behavior) {
        JsonNode featureEncoding = behavior.path("featureEncoding");
        JsonNode sessionEstimator = behavior.path("sessionEstimator");
        JsonNode peakSelection = behavior.path("peakSelection");
        JsonNode startStopPairing = behavior.path("startStopPairing");
        JsonNode ventilation = behavior.path("ventilation");
        JsonNode response = behavior.path("response");

        Map<String, Set<String>> regimeGating = new LinkedHashMap<>();
        JsonNode gatingNode = behavior.path("regimeGating");
        for (String deviceType : List.of("AIR_CONDITIONER", "HEATER")) {
            regimeGating.put(
                    deviceType,
                    Set.copyOf(requiredTextArray(gatingNode.path(deviceType), "behavior.regimeGating." + deviceType))
            );
        }

        return new BehaviorOrchestrationSpec(
                requiredText(behavior, "schemaVersion", "behavior"),
                requiredText(behavior, "timezone", "behavior"),
                requiredTextArray(behavior.path("dailyUsageFeatureOrder"), "behavior.dailyUsageFeatureOrder"),
                Map.copyOf(regimeGating),
                positiveInteger(sessionEstimator, "sameWeekdayMinimumHistoricalDates", "behavior.sessionEstimator"),
                positiveInteger(peakSelection, "hvacMinimumDistanceBins", "behavior.peakSelection"),
                positiveInteger(peakSelection, "ventilationMinimumDistanceBins", "behavior.peakSelection"),
                positiveInteger(startStopPairing, "minimumStopAfterStartBins", "behavior.startStopPairing"),
                positiveInteger(ventilation, "durationMinutes", "behavior.ventilation"),
                requiredText(response, "recommendationType", "behavior.response"),
                nonNegativeInteger(response, "confidenceRoundingDecimals", "behavior.response"),
                featureCycle(featureEncoding, "hour"),
                featureCycle(featureEncoding, "weekday"),
                featureCycle(featureEncoding, "dayOfYear")
        );
    }

    private static int positiveInteger(JsonNode node, String fieldName, String context) {
        int value = node.path(fieldName).asInt(-1);
        if (value <= 0) {
            throw new BundleValidationException("contract 양의 정수 값이 유효하지 않습니다: "
                    + context + "." + fieldName);
        }
        return value;
    }

    private static int nonNegativeInteger(JsonNode node, String fieldName, String context) {
        int value = node.path(fieldName).asInt(-1);
        if (value < 0) {
            throw new BundleValidationException("contract 0 이상 정수 값이 유효하지 않습니다: "
                    + context + "." + fieldName);
        }
        return value;
    }

    private static double featureCycle(JsonNode featureEncoding, String fieldName) {
        String expression = requiredText(featureEncoding, fieldName, "behavior.featureEncoding");
        Matcher matcher = FEATURE_CYCLE_PATTERN.matcher(expression);
        if (!matcher.find()) {
            throw new BundleValidationException("contract 주기 feature 식을 해석할 수 없습니다: "
                    + "behavior.featureEncoding." + fieldName + "=" + expression);
        }
        return Double.parseDouble(matcher.group(1));
    }

    private static String optionalText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        return value == null || value.isBlank() ? null : value;
    }

    private static Integer optionalInteger(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) && node.path(fieldName).canConvertToInt()
                ? node.path(fieldName).intValue()
                : null;
    }

    private static Double optionalDouble(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) && node.path(fieldName).isNumber()
                ? node.path(fieldName).doubleValue()
                : null;
    }

    private static String requiredText(JsonNode node, String fieldName, String context) {
        String value = node.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new BundleValidationException("contract 필수 값이 없습니다: " + context + "." + fieldName);
        }
        return value;
    }

    private static List<String> requiredTextArray(JsonNode node, String context) {
        if (!node.isArray() || node.isEmpty()) {
            throw new BundleValidationException("contract 필수 배열이 비어 있습니다: " + context);
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new BundleValidationException("contract 배열 값이 유효하지 않습니다: " + context);
            }
            values.add(value.asText());
        });
        return values;
    }

    public record OnnxModelSpec(
            String key,
            String filename,
            List<OnnxInputSpec> inputs,
            Set<String> outputs,
            String outputName,
            Integer positiveProbabilityColumn,
            Double threshold
    ) {
    }

    public record OnnxInputSpec(String name, String dtype) {
    }

    public record BehaviorOrchestrationSpec(
            String schemaVersion,
            String timezone,
            List<String> dailyUsageFeatureOrder,
            Map<String, Set<String>> regimeGating,
            int sameWeekdayMinimumHistoricalDates,
            int hvacMinimumDistanceBins,
            int ventilationMinimumDistanceBins,
            int minimumStopAfterStartBins,
            int ventilationDurationMinutes,
            String recommendationType,
            int confidenceRoundingDecimals,
            double hourCycle,
            double weekdayCycle,
            double dayOfYearCycle
    ) {
        public BehaviorOrchestrationSpec {
            dailyUsageFeatureOrder = List.copyOf(dailyUsageFeatureOrder);
            regimeGating = Map.copyOf(regimeGating);
        }
    }
}
