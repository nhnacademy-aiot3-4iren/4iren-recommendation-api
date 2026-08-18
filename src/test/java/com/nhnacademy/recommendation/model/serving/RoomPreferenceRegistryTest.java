package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.RoomPreferenceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomPreferenceRegistryTest {

    @TempDir
    Path tempDirectory;

    @Test
    void loadsUtf8BomAndResolvesCanonicalLocation() throws IOException {
        Path csv = write("\uFEFFroom_id,location,preferred_temperature_c\n1,실습실,23.4\n2,사무실,24.0\n");

        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv),
                Set.of("실습실", "사무실", "회의실")
        );

        assertThat(registry.getRequired(1L).location()).isEqualTo("실습실");
        assertThat(registry.getRequired(1L).values()).containsEntry("preferred_temperature_c", "23.4");
        assertThat(registry.getRequired(2L).location()).isEqualTo("사무실");
    }

    @Test
    void rejectsDuplicateRoomId() throws IOException {
        Path csv = write("room_id,location\n1,실습실\n1,사무실\n");

        assertThatThrownBy(() -> RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv), Set.of("실습실", "사무실")
        ))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("room_id가 중복");
    }

    @Test
    void rejectsBlankLocation() throws IOException {
        Path csv = write("room_id,location\n1,\n");

        assertThatThrownBy(() -> RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv), Set.of("실습실")
        ))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("location이 비어");
    }

    @Test
    void rejectsLocationOutsideContract() throws IOException {
        Path csv = write("room_id,location\n1,임의공간\n");

        assertThatThrownBy(() -> RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv), Set.of("실습실", "사무실", "회의실")
        ))
                .isInstanceOf(BundleValidationException.class)
                .hasMessageContaining("behavior.validLocations");
    }

    @Test
    void rejectsUnknownRoomWithoutFallback() throws IOException {
        Path csv = write("room_id,location\n1,실습실\n");
        RoomPreferenceRegistry registry = RoomPreferenceRegistry.from(
                RuntimeCsvTable.load(csv), Set.of("실습실")
        );

        assertThatThrownBy(() -> registry.getRequired(999L))
                .isInstanceOf(RoomPreferenceNotFoundException.class)
                .hasMessageContaining("roomId:999");
    }

    private Path write(String content) throws IOException {
        Path csv = tempDirectory.resolve("room_preference_profile.csv");
        Files.writeString(csv, content, StandardCharsets.UTF_8);
        return csv;
    }
}
