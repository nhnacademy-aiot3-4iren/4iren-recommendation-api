package com.nhnacademy.recommendation.model.serving;

public interface ModelBundleLoader {

    ValidatedModelBundle loadAndValidate();
}
