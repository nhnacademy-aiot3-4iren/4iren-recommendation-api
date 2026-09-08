package com.nhnacademy.recommendation.model.serving;

import ai.onnxruntime.OrtEnvironment;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.ModelServingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

@Slf4j
public class ModelServingInfrastructure implements AutoCloseable {

    private final MinioModelBundleDownloader bundleDownloader;
    private final ObjectMapper objectMapper;
    private final OrtEnvironment ortEnvironment;
    private final OnnxSmokeTester smokeTester;

    private final ReentrantReadWriteLock runtimeLock = new ReentrantReadWriteLock();

    /**
     * bundle/contract/artifacts/sessions를 하나의 reference로 교체하여
     * 서로 다른 model version의 runtime이 섞이지 않게 합니다.
     */
    private volatile RuntimeState runtime;

    public ModelServingInfrastructure(MinioModelBundleDownloader bundleDownloader,
                                      ObjectMapper objectMapper,
                                      OrtEnvironment ortEnvironment,
                                      OnnxSmokeTester smokeTester) {
        this.bundleDownloader = bundleDownloader;
        this.objectMapper = objectMapper;
        this.ortEnvironment = ortEnvironment;
        this.smokeTester = smokeTester;
    }

    public void initialize() {
        runtimeLock.writeLock().lock();
        try {
            if (runtime != null) {
                return;
            }

            RuntimeState loaded = prepareRuntime(bundleDownloader.downloadAndValidate());
            runtime = loaded;

            log.info("[ModelServing] serving infrastructure startup을 완료했습니다. version={}, sessions={}, roomProfiles={}",
                    loaded.bundle().manifest().modelVersion(),
                    loaded.sessions().size(),
                    loaded.runtimeArtifacts().roomPreferences().profiles().size());
        } finally {
            runtimeLock.writeLock().unlock();
        }
    }

    /**
     * ML nightly가 current.json을 갱신했는지 주기적으로 확인합니다.
     * 새 Bundle 준비가 완전히 성공하기 전까지 기존 runtime은 건드리지 않습니다.
     */
    @Scheduled(fixedDelayString = "${model.serving.reload-interval-ms:60000}")
    public void reloadIfChanged() {
        RuntimeState current = runtime;
        if (current == null) {
            return;
        }

        try {
            Optional<MinioModelBundleDownloader.CurrentModelPointer> pointer =
                    bundleDownloader.currentPointer();

            if (pointer.isEmpty()) {
                log.debug("[ModelServing] current.json이 없어 현재 Bundle을 유지합니다.");
                return;
            }

            String currentVersion = current.bundle().manifest().modelVersion();
            String targetVersion = pointer.get().modelVersion();

            if (currentVersion.equals(targetVersion)) {
                return;
            }

            log.info("[ModelServing] 새 모델 Bundle을 감지했습니다. current={}, target={}",
                    currentVersion, targetVersion);

            // download/validate/session/smoke는 write lock 밖에서 수행합니다.
            // 준비 실패 시 현재 서비스 runtime에는 영향이 없습니다.
            RuntimeState candidate = prepareRuntime(
                    bundleDownloader.downloadAndValidate(pointer.get())
            );

            swapRuntime(candidate);
        } catch (Exception e) {
            log.warn("[ModelServing] 새 모델 Bundle reload에 실패했습니다. 기존 Bundle을 유지합니다. version={}",
                    current.bundle().manifest().modelVersion(), e);
        }
    }

    private RuntimeState prepareRuntime(ValidatedModelBundle loadedBundle) {
        SpringServingContract loadedContract = SpringServingContract.load(
                ModelBundleValidator.resolveInside(
                        loadedBundle.directory(),
                        loadedBundle.manifest().contractFilename()
                ),
                objectMapper
        );

        RuntimeArtifactStore loadedRuntimeArtifacts =
                RuntimeArtifactStore.load(loadedBundle, loadedContract);

        OnnxSessionRegistry loadedSessions = OnnxSessionRegistry.open(
                ortEnvironment,
                loadedBundle.directory(),
                loadedContract
        );

        try {
            smokeTester.run(
                    loadedBundle,
                    loadedContract,
                    loadedSessions
            );
        } catch (Exception e) {
            loadedSessions.close();
            throw e;
        }

        return new RuntimeState(
                loadedBundle,
                loadedContract,
                loadedRuntimeArtifacts,
                loadedSessions
        );
    }

    private void swapRuntime(RuntimeState candidate) {
        runtimeLock.writeLock().lock();
        try {
            RuntimeState previous = requireRuntime();

            if (previous.bundle().manifest().modelVersion()
                    .equals(candidate.bundle().manifest().modelVersion())) {
                candidate.sessions().close();
                return;
            }

            runtime = candidate;

            log.info("[ModelServing] 모델 runtime 교체를 완료했습니다. previous={}, current={}, sessions={}, roomProfiles={}",
                    previous.bundle().manifest().modelVersion(),
                    candidate.bundle().manifest().modelVersion(),
                    candidate.sessions().size(),
                    candidate.runtimeArtifacts().roomPreferences().profiles().size());

            previous.sessions().close();
        } finally {
            runtimeLock.writeLock().unlock();
        }
    }

    /**
     * 추천 요청 하나가 같은 RuntimeState를 끝까지 사용하도록 read lock을 유지합니다.
     */
    public <T> T withRuntime(Function<RuntimeState, T> operation) {
        runtimeLock.readLock().lock();
        try {
            return operation.apply(requireRuntime());
        } finally {
            runtimeLock.readLock().unlock();
        }
    }

    public Optional<RoomPreferenceProfile> findRoomPreference(Long roomId) {
        return withRuntime(state -> state.runtimeArtifacts().roomPreferences().find(roomId));
    }

    public ValidatedModelBundle bundle() {
        return withRuntime(RuntimeState::bundle);
    }

    public SpringServingContract contract() {
        return withRuntime(RuntimeState::contract);
    }

    public RuntimeArtifactStore runtimeArtifacts() {
        return withRuntime(RuntimeState::runtimeArtifacts);
    }

    public OnnxSessionRegistry sessions() {
        return withRuntime(RuntimeState::sessions);
    }

    @Override
    public void close() {
        runtimeLock.writeLock().lock();
        try {
            if (runtime != null) {
                runtime.sessions().close();
                runtime = null;
            }
        } finally {
            runtimeLock.writeLock().unlock();
        }
    }

    private RuntimeState requireRuntime() {
        RuntimeState state = runtime;
        if (state == null) {
            throw new ModelServingException(
                    "Model serving infrastructure가 초기화되지 않았습니다."
            );
        }
        return state;
    }

    public record RuntimeState(
            ValidatedModelBundle bundle,
            SpringServingContract contract,
            RuntimeArtifactStore runtimeArtifacts,
            OnnxSessionRegistry sessions
    ) {
    }
}
