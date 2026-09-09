package com.nhnacademy.recommendation.model.serving;

import java.nio.file.Path;

public record ValidatedModelBundle(
        Path directory,
        BundleManifest manifest
) {
}
