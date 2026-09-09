package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class OnnxBundleSmokeIntegrationTest {

    @Test
    @EnabledIfSystemProperty(named = "model.bundle.test-dir", matches = ".+")
    void validatesAndRunsAllTenOnnxModelsFromRealBundle() {
        Path bundleDirectory = Path.of(System.getProperty("model.bundle.test-dir"));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
        ValidatedModelBundle bundle = new ModelBundleValidator(objectMapper).validate(bundleDirectory);
        SpringServingContract contract = SpringServingContract.load(
                ModelBundleValidator.resolveInside(bundle.directory(), bundle.manifest().contractFilename()),
                objectMapper
        );
        RuntimeArtifactStore runtimeArtifacts = RuntimeArtifactStore.load(bundle, contract);

        try (OrtEnvironment environment = OrtEnvironment.getEnvironment("4iren-model-serving-integration-test");
             OnnxSessionRegistry sessions = OnnxSessionRegistry.open(environment, bundle.directory(), contract)) {
            new OnnxSmokeTester(objectMapper, environment).run(bundle, contract, sessions);

            assertThat(sessions.size()).isEqualTo(10);
            assertThat(runtimeArtifacts.roomPreferences().getRequired(1L).location()).isEqualTo("실습실");
            assertThat(runtimeArtifacts.roomPreferences().getRequired(2L).location()).isEqualTo("사무실");
            assertThat(runtimeArtifacts.roomPreferences().getRequired(3L).location()).isEqualTo("회의실");
        }
    }
}
