package com.nhnacademy.recommendation.model.serving;

import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.RoomPreferenceNotFoundException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public final class RoomPreferenceRegistry {

    private final Map<Long, RoomPreferenceProfile> profiles;

    private RoomPreferenceRegistry(Map<Long, RoomPreferenceProfile> profiles) {
        this.profiles = Map.copyOf(profiles);
    }

    public static RoomPreferenceRegistry from(RuntimeCsvTable table) {
        int roomIdIndex = table.columnIndex("room_id");
        int locationIndex = table.columnIndex("location");
        Map<Long, RoomPreferenceProfile> profiles = new LinkedHashMap<>();

        for (int rowIndex = 0; rowIndex < table.rows().size(); rowIndex++) {
            var row = table.rows().get(rowIndex);
            int csvLine = rowIndex + 2;
            Long roomId = parseRoomId(row.get(roomIdIndex), csvLine);
            String location = row.get(locationIndex);
            if (location == null || location.isBlank()) {
                throw new BundleValidationException(
                        "room_preference_profile.csv location이 비어 있습니다: line=" + csvLine
                );
            }
            Map<String, String> values = new LinkedHashMap<>();
            for (int column = 0; column < table.headers().size(); column++) {
                values.put(table.headers().get(column), row.get(column));
            }
            RoomPreferenceProfile profile = new RoomPreferenceProfile(roomId, location, values);
            RoomPreferenceProfile duplicate = profiles.putIfAbsent(roomId, profile);
            if (duplicate != null) {
                throw new BundleValidationException(
                        "room_preference_profile.csv room_id가 중복됩니다: room_id=" + roomId
                );
            }
        }

        return new RoomPreferenceRegistry(profiles);
    }

    public Optional<RoomPreferenceProfile> find(Long roomId) {
        return roomId == null ? Optional.empty() : Optional.ofNullable(profiles.get(roomId));
    }

    public RoomPreferenceProfile getRequired(Long roomId) {
        return find(roomId).orElseThrow(() -> new RoomPreferenceNotFoundException(roomId));
    }

    public Map<Long, RoomPreferenceProfile> profiles() {
        return profiles;
    }

    private static Long parseRoomId(String value, int csvLine) {
        if (value == null || value.isBlank()) {
            throw new BundleValidationException(
                    "room_preference_profile.csv room_id가 비어 있습니다: line=" + csvLine
            );
        }
        try {
            Long roomId = Long.valueOf(value);
            if (roomId <= 0) {
                throw new BundleValidationException(
                        "room_preference_profile.csv room_id가 양수가 아닙니다: line="
                                + csvLine + " room_id=" + value
                );
            }
            return roomId;
        } catch (NumberFormatException e) {
            throw new BundleValidationException(
                    "room_preference_profile.csv room_id가 Long 형식이 아닙니다: line="
                            + csvLine + " room_id=" + value,
                    e
            );
        }
    }
}
