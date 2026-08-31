package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.ModelServingException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModelServingInfrastructure implements AutoCloseable {

    private final MinioModelBundleDownloader bundleDownloader;
    private final ObjectMapper objectMapper;
    private final OrtEnvironment ortEnvironment;
    private final OnnxSmokeTester smokeTester;

    private volatile ValidatedModelBundle bundle;
    private volatile SpringServingContract contract;
    private volatile RuntimeArtifactStore runtimeArtifacts;
    private volatile OnnxSessionRegistry sessions;

    public ModelServingInfrastructure(MinioModelBundleDownloader bundleDownloader,
                                      ObjectMapper objectMapper,
                                      OrtEnvironment ortEnvironment,
                                      OnnxSmokeTester smokeTester) {
        this.bundleDownloader = bundleDownloader;
        this.objectMapper = objectMapper;
        this.ortEnvironment = ortEnvironment;
        this.smokeTester = smokeTester;
    }

    public synchronized void initialize() {
        if (sessions != null) {
            return;
        }

        ValidatedModelBundle loadedBundle = bundleDownloader.downloadAndValidate();
        SpringServingContract loadedContract = SpringServingContract.load(
                ModelBundleValidator.resolveInside(
                        loadedBundle.directory(), loadedBundle.manifest().contractFilename()
                ),
                objectMapper
        );
        RuntimeArtifactStore loadedRuntimeArtifacts = RuntimeArtifactStore.load(loadedBundle, loadedContract);
        OnnxSessionRegistry loadedSessions = OnnxSessionRegistry.open(
                ortEnvironment,
                loadedBundle.directory(),
                loadedContract
        );

        try {
            smokeTester.run(loadedBundle, loadedContract, loadedSessions);
        } catch (Exception e) {
            loadedSessions.close();
            throw e;
        }

        this.bundle = loadedBundle;
        this.contract = loadedContract;
        this.runtimeArtifacts = loadedRuntimeArtifacts;
        this.sessions = loadedSessions;
        log.info("[ModelServing] serving infrastructure startup을 완료했습니다. version={}, sessions={}, roomProfiles={}",
                loadedBundle.manifest().modelVersion(),
                loadedSessions.size(),
                loadedRuntimeArtifacts.roomPreferences().profiles().size());
    }

    public RoomPreferenceProfile getRoomPreference(Long roomId) {
        RuntimeArtifactStore artifacts = requireInitialized(runtimeArtifacts, "runtime artifacts");
        return artifacts.roomPreferences().getRequired(roomId);
    }

    public ValidatedModelBundle bundle() {
        return requireInitialized(bundle, "bundle");
    }

    public SpringServingContract contract() {
        return requireInitialized(contract, "contract");
    }

    public RuntimeArtifactStore runtimeArtifacts() {
        return requireInitialized(runtimeArtifacts, "runtime artifacts");
    }

    public OnnxSessionRegistry sessions() {
        return requireInitialized(sessions, "ONNX sessions");
    }

    @Override
    public synchronized void close() {
        if (sessions != null) {
            sessions.close();
            sessions = null;
        }
    }

    private <T> T requireInitialized(T value, String resourceName) {
        if (value == null) {
            throw new ModelServingException("Model serving infrastructure가 초기화되지 않았습니다: " + resourceName);
        }
        return value;
    }
}
