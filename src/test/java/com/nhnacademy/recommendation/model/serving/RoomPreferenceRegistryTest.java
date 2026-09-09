package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.exception.BundleValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomPreferenceRegistryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void loadsUtf8BomAndResolvesCanonicalLocation() throws IOException {
        Path csv = write("\uFEFFroom_id,location,preferred_temperature_c\n1,실습실,23.4\n2,사무실,24.0\n");

        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        );

        assertThat(registry.getRequired(1L).location()).isEqualTo("실습실");
        assertThat(registry.getRequired(1L).values()).containsEntry("preferred_temperature_c", "23.4");
        assertThat(registry.getRequired(2L).location()).isEqualTo("사무실");
    }

    @Test
    void rejectsDuplicateRoomId() throws IOException {
        Path csv = write("room_id,location\n1,실습실\n1,사무실\n");

        assertThatThrownBy(() -> RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        ))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("room_id가 중복");
    }

    @Test
    void rejectsBlankLocation() throws IOException {
        Path csv = write("room_id,location\n1,\n");

        assertThatThrownBy(() -> RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        ))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("location이 비어");
    }

    @Test
    void acceptsLocationMetadataOutsideBehaviorTrainingCategories() throws IOException {
        Path csv = write("room_id,location\n8,신규공간\n");

        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        );

        assertThat(registry.find(8L))
                .get()
                .extracting(RoomPreferenceProfile::location)
                .isEqualTo("신규공간");
    }

    @ParameterizedTest
    @ValueSource(longs = {6L, 7L, 8L, 100L})
    void missingRoomCanUseColdStartFallback(long roomId) throws IOException {
        Path csv = write("room_id,location\n1,실습실\n");
        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        );

        assertThat(registry.find(roomId)).isEmpty();
    }

    @Test
    void emptyProfileTableSupportsBaseOnlyColdStart() throws IOException {
        Path csv = write("room_id,location\n");

        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv)
        );

        assertThat(registry.profiles()).isEmpty();
        assertThat(registry.find(100L)).isEmpty();
    }

    private Path write(String content) throws IOException {
        Path csv = tempDirectory.resolve("room_preference_profile.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }
}
