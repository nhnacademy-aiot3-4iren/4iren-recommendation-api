package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.exception.BundleValidationException;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public final class RuntimeArtifactStore {

    private static final List<String> REQUIRED_RUNTIME_DATA = List.of(
            "roomPreferenceProfile",
            "temperatureAdapter",
            "humidityAdapter",
            "behaviorEventHistory",
            "behaviorTemperatureRegimeHistory"
    );

    private final RoomPreferenceRegistry roomPreferenceRegistry;
    private final Map<String, RuntimeCsvTable> csvTables;

    private RuntimeArtifactStore(RoomPreferenceRegistry roomPreferenceRegistry,
                                 Map<String, RuntimeCsvTable> csvTables) {
        this.roomPreferenceRegistry = roomPreferenceRegistry;
        this.csvTables = Map.copyOf(csvTables);
    }

    public static RuntimeArtifactStore load(ValidatedModelBundle bundle,
                                            SpringServingContract contract) {
        if (!bundle.manifest().modelVersion().equals(contract.modelVersion())) {
            throw new BundleValidationException(
                    "manifest와 spring serving contract의 modelVersion이 다릅니다. manifest="
                            + bundle.manifest().modelVersion() + " contract=" + contract.modelVersion()
            );
        }

        String manifestProfile = requiredRuntimeFilename(bundle.manifest(), "roomPreferenceProfile");
        if (!manifestProfile.equals(contract.roomPreferenceFilename())) {
            throw new BundleValidationException(
                    "manifest와 contract의 room preference filename이 다릅니다. manifest="
                            + manifestProfile + " contract=" + contract.roomPreferenceFilename()
            );
        }

        Map<String, RuntimeCsvTable> tables = new LinkedHashMap<>();
        for (String key : REQUIRED_RUNTIME_DATA) {
            String filename = requiredRuntimeFilename(bundle.manifest(), key);
            Path path = ModelBundleValidator.resolveInside(bundle.directory(), filename);
            if (!Files.isRegularFile(path)) {
                throw new BundleValidationException("runtime CSV가 없습니다: " + filename);
            }
            RuntimeCsvTable table = RuntimeCsvTable.load(path);
            tables.put(key, table);
            log.info("[ModelServing] runtime CSV를 메모리에 로드했습니다. key={}, filename={}, rows={}",
                    key, filename, table.rows().size());
        }

        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                tables.get("roomPreferenceProfile"),
                contract.validBehaviorLocations()
        );
        return new RuntimeArtifactStore(registry, tables);
    }

    public RoomPreferenceRegistry roomPreferences() {
        return roomPreferenceRegistry;
    }

    public RuntimeCsvTable csv(String key) {
        RuntimeCsvTable table = csvTables.get(key);
        if (table == null) {
            throw new BundleValidationException("로드되지 않은 runtime CSV key입니다: " + key);
        }
        return table;
    }

    public Map<String, RuntimeCsvTable> csvTables() {
        return csvTables;
    }

    private static String requiredRuntimeFilename(BundleManifest manifest, String key) {
        String filename = manifest.runtimeData().get(key);
        if (filename == null || filename.isBlank()) {
            throw new BundleValidationException("manifest runtimeData 필수 값이 없습니다: " + key);
        }
        return filename;
    }
}
