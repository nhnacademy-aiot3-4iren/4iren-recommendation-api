package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.ModelServingException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
public final class OnnxSessionRegistry implements AutoCloseable {

    private final Map<String, OrtSession> sessions;

    private OnnxSessionRegistry(Map<String, OrtSession> sessions) {
        this.sessions = Map.copyOf(sessions);
    }

    public static OnnxSessionRegistry open(OrtEnvironment environment,
                                           Path bundleDirectory,
                                           SpringServingContract contract) {
        Map<String, OrtSession> sessions = new LinkedHashMap<>();
        try {
            for (SpringServingContract.OnnxModelSpec spec : contract.models().values()) {
                Path modelPath = ModelBundleValidator.resolveInside(bundleDirectory, spec.filename());
                if (!Files.isRegularFile(modelPath)) {
                    throw new BundleValidationException("ONNX 모델 파일이 없습니다: " + spec.filename());
                }
                try (OrtSession.SessionOptions options = new OrtSession.SessionOptions()) {
                    OrtSession session = environment.createSession(modelPath.toString(), options);
                    validateMetadata(spec, session);
                    sessions.put(spec.key(), session);
                    log.info("[ModelServing] ONNX session을 생성했습니다. key={}, filename={}",
                            spec.key(), spec.filename());
                }
            }
            return new OnnxSessionRegistry(sessions);
        } catch (Exception e) {
            closeSessions(sessions);
            if (e instanceof ModelServingException modelServingException) {
                throw modelServingException;
            }
            throw new ModelServingException("ONNX session 생성에 실패했습니다.", e);
        }
    }

    public OrtSession getRequired(String key) {
        OrtSession session = sessions.get(key);
        if (session == null) {
            throw new ModelServingException("등록되지 않은 ONNX session key입니다: " + key);
        }
        return session;
    }

    public int size() {
        return sessions.size();
    }

    @Override
    public void close() {
        closeSessions(sessions);
    }

    private static void validateMetadata(SpringServingContract.OnnxModelSpec spec,
                                         OrtSession session) throws OrtException {
        var actualInputs = session.getInputNames();
        var expectedInputs = spec.inputs().stream()
                .map(SpringServingContract.OnnxInputSpec::name)
                .collect(java.util.stream.Collectors.toSet());
        if (!actualInputs.equals(expectedInputs)) {
            throw new BundleValidationException(
                    "ONNX input metadata가 contract와 다릅니다: " + spec.key()
                            + " expected=" + expectedInputs + " actual=" + actualInputs
            );
        }
        var actualOutputs = session.getOutputNames();
        if (!actualOutputs.equals(spec.outputs())) {
            throw new BundleValidationException(
                    "ONNX output metadata가 contract와 다릅니다: " + spec.key()
                            + " expected=" + spec.outputs() + " actual=" + actualOutputs
            );
        }
    }

    private static void closeSessions(Map<String, OrtSession> sessions) {
        sessions.forEach((key, session) -> {
            try {
                session.close();
            } catch (Exception e) {
                log.warn("[ModelServing] ONNX session 종료에 실패했습니다. key={}", key, e);
            }
        });
    }
}
