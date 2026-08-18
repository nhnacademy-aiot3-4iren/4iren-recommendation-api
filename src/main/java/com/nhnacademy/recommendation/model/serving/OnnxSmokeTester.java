package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.ModelServingException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class OnnxSmokeTester {

    private final ObjectMapper objectMapper;
    private final OrtEnvironment environment;

    public OnnxSmokeTester(ObjectMapper objectMapper, OrtEnvironment environment) {
        this.objectMapper = objectMapper;
        this.environment = environment;
    }

    public void run(ValidatedModelBundle bundle,
                    SpringServingContract contract,
                    OnnxSessionRegistry registry) {
        Path fixturePath = ModelBundleValidator.resolveInside(
                bundle.directory(), bundle.manifest().parityFixtureFilename()
        );
        try {
            JsonNode fixture = objectMapper.readTree(fixturePath.toFile());
            JsonNode coreModels = fixture.path("coreModels");
            Set<String> executed = new HashSet<>();

            runFixtureCase("objective", coreModels.path("objective_classifier"), contract, registry, executed);
            runFixtureCase("temperature", coreModels.path("temperature"), contract, registry, executed);
            runFixtureCase("humidity", coreModels.path("humidity"), contract, registry, executed);

            JsonNode behaviorCase = fixture.path("behavior").path("requests").path(0);
            JsonNode dailyUsageInput = behaviorCase.path("dailyUsageInput");
            if (!dailyUsageInput.isObject()) {
                throw new BundleValidationException("parity fixture에 behavior dailyUsageInput이 없습니다.");
            }
            Map<String, Object> dailyRow = jsonObjectToMap(dailyUsageInput);
            runModel("behavior.dailyUsage.AIR_CONDITIONER", List.of(dailyRow), contract, registry, executed);
            runModel("behavior.dailyUsage.HEATER", List.of(dailyRow), contract, registry, executed);

            List<Map<String, Object>> eventRows = buildEventRows(dailyRow, contract.behaviorBinMinutes());
            runModel("behavior.events.AIR_CONDITIONER_START", eventRows, contract, registry, executed);
            runModel("behavior.events.AIR_CONDITIONER_STOP", eventRows, contract, registry, executed);
            runModel("behavior.events.HEATER_START", eventRows, contract, registry, executed);
            runModel("behavior.events.HEATER_STOP", eventRows, contract, registry, executed);
            runModel("behavior.events.VENTILATION_EVENT", eventRows, contract, registry, executed);

            if (!executed.equals(contract.models().keySet())) {
                throw new BundleValidationException(
                        "smoke inference가 모든 ONNX 모델을 실행하지 않았습니다. expected="
                                + contract.models().keySet() + " actual=" + executed
                );
            }
            log.info("[ModelServing] ONNX 10개 smoke inference를 완료했습니다.");
        } catch (IOException e) {
            throw new BundleValidationException("spring_serving_parity_fixture.json을 읽을 수 없습니다.", e);
        }
    }

    private void runFixtureCase(String modelKey,
                                JsonNode fixtureModel,
                                SpringServingContract contract,
                                OnnxSessionRegistry registry,
                                Set<String> executed) {
        JsonNode inputs = fixtureModel.path("cases").path(0).path("inputs");
        if (!inputs.isObject()) {
            throw new BundleValidationException("parity fixture core model input이 없습니다: " + modelKey);
        }
        runModel(modelKey, List.of(jsonObjectToMap(inputs)), contract, registry, executed);
    }

    private void runModel(String modelKey,
                          List<Map<String, Object>> rows,
                          SpringServingContract contract,
                          OnnxSessionRegistry registry,
                          Set<String> executed) {
        SpringServingContract.OnnxModelSpec spec = contract.models().get(modelKey);
        if (spec == null) {
            throw new BundleValidationException("contract에 smoke 대상 모델이 없습니다: " + modelKey);
        }

        Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
        try {
            for (SpringServingContract.OnnxInputSpec inputSpec : spec.inputs()) {
                inputs.put(inputSpec.name(), createTensor(inputSpec, rows));
            }
            OrtSession session = registry.getRequired(modelKey);
            try (OrtSession.Result result = session.run(inputs)) {
                for (String outputName : spec.outputs()) {
                    var output = result.get(outputName)
                            .orElseThrow(() -> new BundleValidationException(
                                    "smoke inference output이 없습니다: model=" + modelKey + " output=" + outputName
                            ));
                    if (output.getValue() == null) {
                        throw new BundleValidationException(
                                "smoke inference output 값이 null입니다: model=" + modelKey + " output=" + outputName
                        );
                    }
                }
            }
            executed.add(modelKey);
        } catch (ModelServingException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelServingException("ONNX smoke inference에 실패했습니다: " + modelKey, e);
        } finally {
            inputs.values().forEach(tensor -> {
                try {
                    tensor.close();
                } catch (Exception e) {
                    log.warn("[ModelServing] smoke input tensor 종료에 실패했습니다. model={}", modelKey, e);
                }
            });
        }
    }

    private OnnxTensor createTensor(SpringServingContract.OnnxInputSpec inputSpec,
                                    List<Map<String, Object>> rows) throws Exception {
        if ("tensor(string)".equals(inputSpec.dtype())) {
            String[][] values = new String[rows.size()][1];
            for (int row = 0; row < rows.size(); row++) {
                Object value = requiredInputValue(rows.get(row), inputSpec.name());
                values[row][0] = value == null ? "nan" : value.toString();
            }
            return OnnxTensor.createTensor(environment, values);
        }
        if ("tensor(float)".equals(inputSpec.dtype())) {
            float[][] values = new float[rows.size()][1];
            for (int row = 0; row < rows.size(); row++) {
                Object value = requiredInputValue(rows.get(row), inputSpec.name());
                values[row][0] = value == null ? Float.NaN : ((Number) value).floatValue();
            }
            return OnnxTensor.createTensor(environment, values);
        }
        throw new BundleValidationException(
                "smoke inference가 지원하지 않는 input dtype입니다: " + inputSpec.dtype()
        );
    }

    private Object requiredInputValue(Map<String, Object> row, String inputName) {
        if (!row.containsKey(inputName)) {
            throw new BundleValidationException("parity fixture에 ONNX input 값이 없습니다: " + inputName);
        }
        return row.get(inputName);
    }

    private Map<String, Object> jsonObjectToMap(JsonNode node) {
        Map<String, Object> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isNull()) {
                values.put(entry.getKey(), null);
            } else if (value.isNumber()) {
                values.put(entry.getKey(), value.floatValue());
            } else {
                values.put(entry.getKey(), value.asText());
            }
        });
        return values;
    }

    private List<Map<String, Object>> buildEventRows(Map<String, Object> dailyRow, int binMinutes) {
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int minute = 0; minute < 1440; minute += binMinutes) {
            double hour = minute / 60.0;
            Map<String, Object> row = new LinkedHashMap<>(dailyRow);
            row.put("hour_sin", (float) Math.sin(2.0 * Math.PI * hour / 24.0));
            row.put("hour_cos", (float) Math.cos(2.0 * Math.PI * hour / 24.0));
            rows.add(row);
        }
        return rows;
    }
}
