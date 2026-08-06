CREATE TABLE IF NOT EXISTS team_indoor_policy (
    team_id BIGINT NOT NULL,
    co2_warning_ppm INT NULL,
    co2_danger_ppm INT NULL,
    humidity_low_percent DOUBLE NULL,
    humidity_high_percent DOUBLE NULL,
    temperature_low_celsius DOUBLE NULL,
    temperature_high_celsius DOUBLE NULL,
    pm25_warning DOUBLE NULL,
    pm10_warning DOUBLE NULL,
    strong_wind_speed DOUBLE NULL,
    stale_sensor_minutes INT NULL,
    rain_possible_probability INT NULL,
    rain_expected_probability INT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
