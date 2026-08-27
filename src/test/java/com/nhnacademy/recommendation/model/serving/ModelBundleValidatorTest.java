package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ModelBundleValidatorTest {

    @TempDir
    Path bundleDirectory;

    @Test
    void validatesArtifactSizeAndChecksum() throws IOException {
        Path contract = bundleDirectory.resolve("spring_serving_contract.json");
        Files.writeString(contract, "{}", StandardCharsets.UTF_8);
        writeManifest(ModelBundleValidator.sha256(contract));

        ValidatedModelBundle bundle = new ModelBundleValidator(new ObjectMapper()).validate(bundleDirectory);

        assertThat(bundle.manifest().modelVersion()).isEqualTo("v1");
    }

    @Test
    void rejectsChecksumMismatch() throws IOException {
        Path contract = bundleDirectory.resolve("spring_serving_contract.json");
        Files.writeString(contract, "{}", StandardCharsets.UTF_8);
        writeManifest("0".repeat(64));

        assertThatThrownBy(() -> new ModelBundleValidator(new ObjectMapper()).validate(bundleDirectory))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("checksum");
    }

    private void writeManifest(String checksum) throws IOException {
        String manifest = """
                {
                  "modelVersion": "v1",
                  "artifactFiles": ["spring_serving_contract.json", "manifest.json"],
                  "artifacts": {
                    "spring_serving_contract.json": {
                      "sha256": "%s",
                      "sizeBytes": 2
                    }
                  },
                  "servingArtifacts": {"contract": "spring_serving_contract.json"},
                  "validationReports": {"springServingParity": "spring_serving_parity_fixture.json"},
                  "runtimeData": {}
                }
                """.formatted(checksum);
        Files.writeString(bundleDirectory.resolve("manifest.json"), manifest, StandardCharsets.UTF_8);
    }
}
