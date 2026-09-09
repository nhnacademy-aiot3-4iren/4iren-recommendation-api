package com.nhnacademy.recommendation.model.serving;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhnacademy.recommendation.exception.BundleValidationException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record BundleManifest(
        String modelVersion,
        List<String> artifactFiles,
        Map<String, ArtifactMetadata> artifacts,
        String contractFilename,
        String parityFixtureFilename,
        Map<String, String> runtimeData
) {

    public static BundleManifest load(Path manifestPath, ObjectMapper objectMapper) {
        try {
            JsonNode root = objectMapper.readTree(manifestPath.toFile());
            String modelVersion = requiredText(root, "modelVersion");
            List<String> artifactFiles = requiredTextArray(root.path("artifactFiles"), "artifactFiles");
            if (new HashSet<>(artifactFiles).size() != artifactFiles.size()) {
                throw new BundleValidationException("manifest.json의 artifactFiles에 중복 경로가 있습니다.");
            }

            Map<String, ArtifactMetadata> artifacts = new LinkedHashMap<>();
            JsonNode artifactNode = root.path("artifacts");
            if (!artifactNode.isObject()) {
                throw new BundleValidationException("manifest.json의 artifacts가 객체가 아닙니다.");
            }
            artifactNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                String sha256 = requiredText(value, "sha256");
                long sizeBytes = value.path("sizeBytes").asLong(-1);
                if (sizeBytes < 0) {
                    throw new BundleValidationException("artifact sizeBytes가 유효하지 않습니다: " + entry.getKey());
                }
                artifacts.put(entry.getKey(), new ArtifactMetadata(sha256, sizeBytes));
            });

            String contractFilename = requiredText(root.path("servingArtifacts"), "contract");
            String parityFixtureFilename = requiredText(root.path("validationReports"), "springServingParity");
            Map<String, String> runtimeData = requiredTextMap(root.path("runtimeData"), "runtimeData");

            return new BundleManifest(
                    modelVersion,
                    List.copyOf(artifactFiles),
                    Map.copyOf(artifacts),
                    contractFilename,
                    parityFixtureFilename,
                    Map.copyOf(runtimeData)
            );
        } catch (IOException e) {
            throw new BundleValidationException("manifest.json을 읽을 수 없습니다: " + manifestPath, e);
        }
    }

    private static String requiredText(JsonNode node, String fieldName) {
        String value = node.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            throw new BundleValidationException("manifest.json 필수 값이 없습니다: " + fieldName);
        }
        return value;
    }

    private static List<String> requiredTextArray(JsonNode node, String fieldName) {
        if (!node.isArray() || node.isEmpty()) {
            throw new BundleValidationException("manifest.json 필수 배열이 비어 있습니다: " + fieldName);
        }
        List<String> values = new ArrayList<>();
        node.forEach(value -> {
            if (!value.isTextual() || value.asText().isBlank()) {
                throw new BundleValidationException("manifest.json 배열 값이 유효하지 않습니다: " + fieldName);
            }
            values.add(value.asText());
        });
        return values;
    }

    private static Map<String, String> requiredTextMap(JsonNode node, String fieldName) {
        if (!node.isObject()) {
            throw new BundleValidationException("manifest.json 필수 객체가 없습니다: " + fieldName);
        }
        Map<String, String> values = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            if (!entry.getValue().isTextual() || entry.getValue().asText().isBlank()) {
                throw new BundleValidationException("manifest.json 객체 값이 유효하지 않습니다: " + fieldName + "." + entry.getKey());
            }
            values.put(entry.getKey(), entry.getValue().asText());
        });
        return values;
    }

    public record ArtifactMetadata(String sha256, long sizeBytes) {
    }
}
