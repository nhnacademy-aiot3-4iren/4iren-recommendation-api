package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.config.ModelServingProperties;
import com.nhnacademy.recommendation.exception.ModelServingException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;

@Slf4j
public class LocalModelBundleLoader implements ModelBundleLoader {

    private final ModelServingProperties properties;
    private final ModelBundleValidator validator;

    public LocalModelBundleLoader(ModelServingProperties properties, ModelBundleValidator validator) {
        this.properties = properties;
        this.validator = validator;
    }

    @Override
    public ValidatedModelBundle loadAndValidate() {
        String configured = properties.getBundleDirectory();
        if (configured == null || configured.isBlank()) {
            throw new ModelServingException("LOCAL 모델 source에는 model.serving.bundle-directory가 필요합니다.");
        }
        Path directory = Path.of(configured).toAbsolutePath().normalize();
        ValidatedModelBundle bundle = validator.validate(directory);
        log.info("[ModelServing] local candidate Bundle 검증을 완료했습니다. version={}, directory={}",
                bundle.manifest().modelVersion(), bundle.directory());
        return bundle;
    }
}
