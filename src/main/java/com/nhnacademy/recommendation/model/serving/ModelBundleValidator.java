package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class ModelBundleValidator {

    private static final String MANIFEST_FILENAME = "manifest.json";

    private final ObjectMapper objectMapper;

    public ModelBundleValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedModelBundle validate(Path bundleDirectory) {
        Path normalizedBundle = bundleDirectory.toAbsolutePath().normalize();
        Path manifestPath = resolveInside(normalizedBundle, MANIFEST_FILENAME);
        if (!Files.isRegularFile(manifestPath)) {
            throw new BundleValidationException("모델 Bundle에 manifest.json이 없습니다: " + normalizedBundle);
        }

        BundleManifest manifest = BundleManifest.load(manifestPath, objectMapper);
        for (String filename : manifest.artifactFiles()) {
            Path artifactPath = resolveInside(normalizedBundle, filename);
            if (!Files.isRegularFile(artifactPath)) {
                throw new BundleValidationException("manifest artifact가 없습니다: " + filename);
            }
            if (MANIFEST_FILENAME.equals(filename)) {
                continue;
            }

            BundleManifest.ArtifactMetadata metadata = manifest.artifacts().get(filename);
            if (metadata == null) {
                throw new BundleValidationException("manifest checksum 정보가 없습니다: " + filename);
            }
            validateSizeAndChecksum(artifactPath, filename, metadata);
        }
        return new ValidatedModelBundle(normalizedBundle, manifest);
    }

    public static Path resolveInside(Path bundleDirectory, String filename) {
        if (filename == null || filename.isBlank()) {
            throw new BundleValidationException("Bundle artifact 경로가 비어 있습니다.");
        }
        Path relative = Path.of(filename);
        if (relative.isAbsolute()) {
            throw new BundleValidationException("Bundle artifact는 상대 경로여야 합니다: " + filename);
        }
        Path resolved = bundleDirectory.resolve(relative).normalize();
        if (!resolved.startsWith(bundleDirectory)) {
            throw new BundleValidationException("Bundle 외부를 가리키는 artifact 경로입니다: " + filename);
        }
        return resolved;
    }

    private void validateSizeAndChecksum(Path artifactPath,
                                         String filename,
                                         BundleManifest.ArtifactMetadata metadata) {
        try {
            long actualSize = Files.size(artifactPath);
            if (actualSize != metadata.sizeBytes()) {
                throw new BundleValidationException(
                        "artifact 크기가 manifest와 다릅니다: " + filename
                                + " expected=" + metadata.sizeBytes() + " actual=" + actualSize
                );
            }
            String actualSha256 = sha256(artifactPath);
            if (!actualSha256.equalsIgnoreCase(metadata.sha256())) {
                throw new BundleValidationException(
                        "artifact checksum이 manifest와 다릅니다: " + filename
                                + " expected=" + metadata.sha256() + " actual=" + actualSha256
                );
            }
        } catch (IOException e) {
            throw new BundleValidationException("artifact 검증에 실패했습니다: " + filename, e);
        }
    }

    public static String sha256(Path path) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(path)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 MessageDigest를 사용할 수 없습니다.", e);
        } catch (IOException e) {
            throw new BundleValidationException("checksum 계산에 실패했습니다: " + path, e);
        }
    }
}
