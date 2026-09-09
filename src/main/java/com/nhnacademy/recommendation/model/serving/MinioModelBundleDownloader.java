package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.config.ModelServingProperties;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.ModelServingException;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.errors.ErrorResponseException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Optional;

@Slf4j
public class MinioModelBundleDownloader {

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

    /**
     * Startup에서는 current.json을 우선 사용하고, 아직 pointer가 없는 최초
     * bootstrap 상황에만 기존 고정 prefix를 fallback으로 사용합니다.
     */
    public ValidatedModelBundle downloadAndValidate() {
        Optional<CurrentModelPointer> pointer = currentPointer();
        if (pointer.isPresent()) {
            return downloadAndValidate(pointer.get());
        }
        log.warn("[ModelServing] MinIO current pointer가 없어 fallback prefix를 사용합니다. prefix={}",
                properties.getMinio().getPrefix());
        return downloadAndValidatePrefix(properties.getMinio().getPrefix(), null);
    }

    public ValidatedModelBundle downloadAndValidate(CurrentModelPointer pointer) {
        return downloadAndValidatePrefix(pointer.servingPrefix(), pointer.modelVersion());
    }

    /**
     * MinIO bucket root의 current.json을 읽습니다.
     * 아직 생성되지 않은 초기 상태는 Optional.empty()로 취급합니다.
     */
    public Optional<CurrentModelPointer> currentPointer() {
        validateConfiguration();

        String objectName = properties.getMinio().getCurrentPointerObject().trim();
        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getMinio().getBucket())
                        .object(objectName)
                        .build()
        )) {
            JsonNode root = objectMapper.readTree(response);
            String modelVersion = requiredText(root, "modelVersion");
            String servingPrefix = normalizePrefix(requiredText(root, "servingPrefix"));

            String expectedPrefix = "versions/" + modelVersion;
            if (!expectedPrefix.equals(servingPrefix)) {
                throw new BundleValidationException(
                        "current.json modelVersion과 servingPrefix가 일치하지 않습니다: "
                                + "modelVersion=" + modelVersion
                                + ", servingPrefix=" + servingPrefix
                );
            }

            return Optional.of(new CurrentModelPointer(modelVersion, servingPrefix));
        } catch (ErrorResponseException e) {
            String code = e.errorResponse() == null ? null : e.errorResponse().code();
            if ("NoSuchKey".equals(code) || "NoSuchObject".equals(code)) {
                return Optional.empty();
            }
            throw new ModelServingException("MinIO current pointer 조회에 실패했습니다.", e);
        } catch (ModelServingException e) {
            throw e;
        } catch (Exception e) {
            throw new ModelServingException("MinIO current pointer 조회에 실패했습니다.", e);
        }
    }

    private ValidatedModelBundle downloadAndValidatePrefix(String rawPrefix,
                                                           String expectedModelVersion) {
        validateConfiguration();

        String prefix = normalizePrefix(rawPrefix);
        Path cacheDirectory = properties.resolveCacheDirectory().toAbsolutePath().normalize();
        Path stagingDirectory = null;

        try {
            Files.createDirectories(cacheDirectory);
            stagingDirectory = Files.createTempDirectory(cacheDirectory, ".bundle-download-");

            downloadObject(prefix, MANIFEST_FILENAME, stagingDirectory);

            BundleManifest manifest = BundleManifest.load(
                    stagingDirectory.resolve(MANIFEST_FILENAME),
                    objectMapper
            );

            if (expectedModelVersion != null
                    && !expectedModelVersion.equals(manifest.modelVersion())) {
                throw new BundleValidationException(
                        "current pointer와 Bundle modelVersion이 다릅니다: "
                                + "pointer=" + expectedModelVersion
                                + ", bundle=" + manifest.modelVersion()
                );
            }

            for (String filename : manifest.artifactFiles()) {
                if (!MANIFEST_FILENAME.equals(filename)) {
                    downloadObject(prefix, filename, stagingDirectory);
                }
            }

            ValidatedModelBundle stagedBundle = validator.validate(stagingDirectory);
            String manifestHash = ModelBundleValidator.sha256(
                    stagingDirectory.resolve(MANIFEST_FILENAME)
            );

            Path finalDirectory = cacheDirectory.resolve(
                    sanitizeDirectoryName(stagedBundle.manifest().modelVersion())
                            + "-"
                            + manifestHash.substring(0, 12)
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

            log.info("[ModelServing] MinIO 모델 Bundle 다운로드와 검증을 완료했습니다. version={}, prefix={}, directory={}",
                    stagedBundle.manifest().modelVersion(), prefix, finalDirectory);

            return validator.validate(finalDirectory);
        } catch (ModelServingException e) {
            cleanupStaging(stagingDirectory);
            throw e;
        } catch (Exception e) {
            cleanupStaging(stagingDirectory);
            throw new ModelServingException("MinIO 모델 Bundle 다운로드에 실패했습니다.", e);
        }
    }

    private void downloadObject(String prefix,
                                String filename,
                                Path stagingDirectory) throws Exception {
        Path target = ModelBundleValidator.resolveInside(stagingDirectory, filename);
        Files.createDirectories(target.getParent());

        try (GetObjectResponse response = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(properties.getMinio().getBucket())
                        .object(prefix + "/" + filename)
                        .build()
        )) {
            Files.copy(response, target);
        }
    }

    private String normalizePrefix(String value) {
        if (value == null) {
            throw new ModelServingException("MinIO 모델 Bundle prefix가 없습니다.");
        }

        String prefix = value.trim();
        while (prefix.startsWith("/")) {
            prefix = prefix.substring(1);
        }
        while (prefix.endsWith("/")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        if (prefix.isBlank()) {
            throw new ModelServingException("MinIO 모델 Bundle prefix가 유효하지 않습니다.");
        }
        return prefix;
    }

    private String requiredText(JsonNode root, String fieldName) {
        String value = root.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new BundleValidationException(
                    "current.json 필수 값이 없습니다: " + fieldName
            );
        }
        return value;
    }

    private void validateConfiguration() {
        requireText(properties.getMinio().getEndpoint(), "model.serving.minio.endpoint");
        requireText(properties.getMinio().getAccessKey(), "model.serving.minio.access-key");
        requireText(properties.getMinio().getSecretKey(), "model.serving.minio.secret-key");
        requireText(properties.getMinio().getBucket(), "model.serving.minio.bucket");
        requireText(properties.getMinio().getPrefix(), "model.serving.minio.prefix");
        requireText(properties.getMinio().getCurrentPointerObject(),
                "model.serving.minio.current-pointer-object");
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ModelServingException(
                    "모델 serving 필수 설정이 없습니다: " + propertyName
            );
        }
    }

    private String sanitizeDirectoryName(String value) {
        String sanitized = value.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (sanitized.isBlank()) {
            throw new BundleValidationException(
                    "modelVersion을 cache 디렉터리 이름으로 사용할 수 없습니다: " + value
            );
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

    public record CurrentModelPointer(
            String modelVersion,
            String servingPrefix
    ) {
    }
}
