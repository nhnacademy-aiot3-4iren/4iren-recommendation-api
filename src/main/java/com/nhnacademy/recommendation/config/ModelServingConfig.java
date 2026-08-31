package com.nhnacademy.recommendation.config;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.ModelServingException;
import com.nhnacademy.recommendation.model.serving.MinioModelBundleDownloader;
import com.nhnacademy.recommendation.model.serving.LocalModelBundleLoader;
import com.nhnacademy.recommendation.model.serving.ModelBundleLoader;
import com.nhnacademy.recommendation.model.serving.ModelBundleValidator;
import com.nhnacademy.recommendation.model.serving.ModelServingInfrastructure;
import com.nhnacademy.recommendation.model.serving.OnnxSmokeTester;
import io.minio.MinioClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ModelServingProperties.class)
public class ModelServingConfig {

    @Bean
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "model.serving", name = "source", havingValue = "MINIO", matchIfMissing = true)
    public MinioClient modelBundleMinioClient(ModelServingProperties properties) {
        requireText(properties.getMinio().getEndpoint(), "model.serving.minio.endpoint");
        requireText(properties.getMinio().getAccessKey(), "model.serving.minio.access-key");
        requireText(properties.getMinio().getSecretKey(), "model.serving.minio.secret-key");
        return MinioClient.builder()
                .endpoint(properties.getMinio().getEndpoint())
                .credentials(properties.getMinio().getAccessKey(), properties.getMinio().getSecretKey())
                .build();
    }

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OrtEnvironment modelOrtEnvironment() {
        return OrtEnvironment.getEnvironment("4iren-model-serving");
    }

    @Bean
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "model.serving", name = "source", havingValue = "MINIO", matchIfMissing = true)
    public MinioModelBundleDownloader minioModelBundleDownloader(MinioClient modelBundleMinioClient,
                                                                 ModelServingProperties properties,
                                                                 ModelBundleValidator validator,
                                                                 ObjectMapper objectMapper) {
        return new MinioModelBundleDownloader(modelBundleMinioClient, properties, validator, objectMapper);
    }

    @Bean
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    @ConditionalOnProperty(prefix = "model.serving", name = "source", havingValue = "LOCAL")
    public LocalModelBundleLoader localModelBundleLoader(ModelServingProperties properties,
                                                         ModelBundleValidator validator) {
        return new LocalModelBundleLoader(properties, validator);
    }

    @Bean
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    public OnnxSmokeTester onnxSmokeTester(ObjectMapper objectMapper, OrtEnvironment modelOrtEnvironment) {
        return new OnnxSmokeTester(objectMapper, modelOrtEnvironment);
    }

    @Bean(initMethod = "initialize", destroyMethod = "close")
    @ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
    public ModelServingInfrastructure modelServingInfrastructure(
            ModelBundleLoader modelBundleLoader,
            ObjectMapper objectMapper,
            OrtEnvironment modelOrtEnvironment,
            OnnxSmokeTester onnxSmokeTester) {
        return new ModelServingInfrastructure(
                modelBundleLoader,
                objectMapper,
                modelOrtEnvironment,
                onnxSmokeTester
        );
    }

    private void requireText(String value, String propertyName) {
        if (value == null || value.isBlank()) {
            throw new ModelServingException("모델 serving 필수 설정이 없습니다: " + propertyName);
        }
    }
}
