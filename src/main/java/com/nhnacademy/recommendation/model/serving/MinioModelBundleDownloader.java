package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.config.ModelServingProperties;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.ModelServingException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;

@Slf4j
public class MinioModelBundleDownloader implements ModelBundleLoader {

    private static final String MANIFEST_FILENAME = "manifest.json";

    private final MinioClient minioClient;
    private final ModelServingProperties properties;
    private final ModelBundleValidator validator;
    private final ObjectMapper objectMapper;

    public MinioModelBundleDownloader(MinioClient minioClient,
                                      ModelServingProperties properties,
                                      ModelBundleValidator validator,
                                      ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.validator = validator;
        this.objectMapper = objectMapper;
    }

    @Override
    public ValidatedModelBundle loadAndValidate() {
        validateConfiguration();
        Path cacheDirectory = properties.resolveCacheDirectory().toAbsolutePath().normalize();
        Path stagingDirectory = null;
        try {
            Files.createDirectories(cacheDirectory);
            stagingDirectory = Files.createTempDirectory(cacheDirectory, ".bundle-download-");
            downloadObject(MANIFEST_FILENAME, stagingDirectory);

            BundleManifest manifest = BundleManifest.load(
                    stagingDirectory.resolve(MANIFEST_FILENAME),
                    objectMapper
            );
            for (String filename : manifest.artifactFiles()) {
                if (!MANIFEST_FILENAME.equals(filename)) {
                    downloadObject(filename, stagingDirectory);
                }
            }

            ValidatedModelBundle stagedBundle = validator.validate(stagingDirectory);
            String manifestHash = ModelBundleValidator.sha256(stagingDirectory.resolve(MANIFEST_FILENAME));
            Path finalDirectory = cacheDirectory.resolve(
                    sanitizeDirectoryName(stagedBundle.manifest().modelVersion()) + "-" + manifestHash.substring(0, 12)
            );

            if (Files.exists(finalDirectory)) {
                ValidatedModelBundle cachedBundle = validator.validate(finalDirectory);
                deleteRecursively(stagingDirectory);
                log.info("[ModelServing] 검증된 모델 Bundle cache를 재사용합니다. version={}, directory={}",
                        cachedBundle.manifest().modelVersion(), finalDirectory);
                return cachedBundle;
            }

            try {
                moveDirectory(stagingDirectory, finalDirectory);
            } catch (FileAlreadyExistsException race) {
                ValidatedModelBundle cachedBundle = validator.validate(finalDirectory);
                deleteRecursively(stagingDirectory);
                log.info("[ModelServing] 동시에 생성된 모델 Bundle cache를 재사용합니다. version={}, directory={}",
                        cachedBundle.manifest().modelVersion(), finalDirectory);
                return cachedBundle;
            }
            log.info("[ModelServing] MinIO 모델 Bundle 다운로드와 검증을 완료했습니다. version={}, directory={}",
                    stagedBundle.manifest().modelVersion(), finalDirectory);
            return validator.validate(finalDirectory);
        } catch (ModelServingException e) {
            cleanupStaging(stagingDirectory);
            throw e;
        } catch (Exception e) {
            cleanupStaging(stagingDirectory);
            throw new ModelServingException("MinIO 모델 Bundle 다운로드에 실패했습니다.", e);
        }
    }

    /**
     * @deprecated use {@link #loadAndValidate()} through {@link ModelBundleLoader}.
     */
    @Deprecated(forRemoval = false)
    public ValidatedModelBundle downloadAndValidate() {
        return loadAndValidate();
    }

    private void downloadObject(String filename, Path stagingDirectory) throws Exception {
        Path target = ModelBundleValidator.resolveInside(stagingDirectory, filename);
        Files.createDirectories(target.getParent());
        String objectName = objectName(filename);
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getMinio().getBucket())
                        .object(objectName)
                        .build()
        )) {
            Files.copy(response, target);
        }
    }

    private String objectName(String filename) {
        String prefix = properties.getMinio().getPrefix().trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        if (prefix.isBlank()) {
            throw new ModelServingException("MinIO 모델 Bundle prefix가 유효하지 않습니다.");
        }
        return prefix + "/" + filename;
    }

    private void validateConfiguration() {
        requireText(properties.getMinio().getEndpoint(), "model.serving.minio.endpoint");
        requireText(properties.getMinio().getAccessKey(), "model.serving.minio.access-key");
        requireText(properties.getMinio().getSecretKey(), "model.serving.minio.secret-key");
        requireText(properties.getMinio().getBucket(), "model.serving.minio.bucket");
        requireText(properties.getMinio().getPrefix(), "model.serving.minio.prefix");
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ModelServingException("모델 serving 필수 설정이 없습니다: " + propertyName);
        }
    }

    private String sanitizeDirectoryName(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            throw new BundleValidationException("modelVersion을 cache 디렉터리 이름으로 사용할 수 없습니다: " + value);
        }
        return sanitized;
    }

    private void moveDirectory(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(source, target);
        }
    }

    private void cleanupStaging(Path stagingDirectory) {
        if (stagingDirectory == null || !Files.exists(stagingDirectory)) {
            return;
        }
        try {
            deleteRecursively(stagingDirectory);
        } catch (IOException cleanupError) {
            log.warn("[ModelServing] 실패한 Bundle staging 디렉터리를 정리하지 못했습니다. directory={}",
                    stagingDirectory, cleanupError);
        }
    }

    private void deleteRecursively(Path directory) throws IOException {
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
