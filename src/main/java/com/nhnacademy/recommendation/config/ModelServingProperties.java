package com.nhnacademy.recommendation.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@Getter
@Setter
@ConfigurationProperties(prefix = "model.serving")
public class ModelServingProperties {

    private boolean enabled = true;
    private Source source = Source.MINIO;
    private String bundleDirectory;
    private final Minio minio = new Minio();
    private String cacheDirectory;

    public Path resolveCacheDirectory() {
        if (cacheDirectory == null || cacheDirectory.isBlank()) {
            return Path.of(System.getProperty("java.io.tmpdir"), "4iren-model-bundles");
        }
        return Path.of(cacheDirectory);
    }

    public enum Source {
        MINIO,
        LOCAL
    }

    @Getter
    @Setter
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucket = "4iren-models";
        private String prefix = "versions/v1";
    }
}
