package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ModelServingReloadIntegrationTest {

    @Test
    void reloadsFrom20260825To20260831() throws Exception {
        String previousDirProperty =
                System.getProperty("model.bundle.reload-from-dir");
        String latestDirProperty =
                System.getProperty("model.bundle.test-dir");

        assumeTrue(previousDirProperty != null
                        && !previousDirProperty.isBlank(),
                "model.bundle.reload-from-dir is required");

        assumeTrue(latestDirProperty != null
                        && !latestDirProperty.isBlank(),
                "model.bundle.test-dir is required");

        Path previousDirectory = Path.of(previousDirProperty)
                .toAbsolutePath()
                .normalize();

        Path latestDirectory = Path.of(latestDirProperty)
                .toAbsolutePath()
                .normalize();

        ObjectMapper objectMapper =
                new ObjectMapper().findAndRegisterModules();

        ModelBundleValidator validator =
                new ModelBundleValidator(objectMapper);

        ValidatedModelBundle previousBundle =
                validator.validate(previousDirectory);

        ValidatedModelBundle latestBundle =
                validator.validate(latestDirectory);

        assertThat(previousBundle.manifest().modelVersion())
                .isEqualTo("models-2026-08-25");

        assertThat(latestBundle.manifest().modelVersion())
                .isEqualTo("models-2026-08-31");

        MinioModelBundleDownloader downloader =
                new MinioModelBundleDownloader(null, null, null, null) {

                    @Override
                    public ValidatedModelBundle downloadAndValidate() {
                        return previousBundle;
                    }

                    @Override
                    public Optional<CurrentModelPointer> currentPointer() {
                        return Optional.of(
                                new CurrentModelPointer(
                                        "models-2026-08-31",
                                        "versions/models-2026-08-31"
                                )
                        );
                    }

                    @Override
                    public ValidatedModelBundle downloadAndValidate(
                            CurrentModelPointer pointer) {

                        assertThat(pointer.modelVersion())
                                .isEqualTo("models-2026-08-31");

                        assertThat(pointer.servingPrefix())
                                .isEqualTo("versions/models-2026-08-31");

                        return latestBundle;
                    }
                };

        try (OrtEnvironment environment =
                     OrtEnvironment.getEnvironment(
                             "model-serving-reload-integration-test")) {

            ModelServingInfrastructure infrastructure =
                    new ModelServingInfrastructure(
                            downloader,
                            objectMapper,
                            environment,
                            new OnnxSmokeTester(objectMapper, environment)
                    );

            try {
                infrastructure.initialize();

                assertThat(
                        infrastructure.bundle()
                                .manifest()
                                .modelVersion()
                ).isEqualTo("models-2026-08-25");

                assertThat(infrastructure.sessions().size())
                        .isEqualTo(10);

                infrastructure.reloadIfChanged();

                assertThat(
                        infrastructure.bundle()
                                .manifest()
                                .modelVersion()
                ).isEqualTo("models-2026-08-31");

                assertThat(infrastructure.bundle().directory())
                        .isEqualTo(latestDirectory);

                assertThat(infrastructure.sessions().size())
                        .isEqualTo(10);

            } finally {
                infrastructure.close();
            }
        }
    }
}
