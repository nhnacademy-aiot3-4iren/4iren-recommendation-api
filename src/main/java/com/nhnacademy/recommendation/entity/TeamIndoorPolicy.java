package com.nhnacademy.recommendation.entity;

import com.nhnacademy.recommendation.dto.welcomeBriefing.TeamIndoorPolicyOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "team_indoor_policy")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeamIndoorPolicy {

    @Id
    @Column(name = "team_id", nullable = false)
    private Long teamId;

    @Column(name = "co2_warning_ppm")
    private Integer co2WarningPpm;

    @Column(name = "co2_danger_ppm")
    private Integer co2DangerPpm;

    @Column(name = "humidity_low_percent")
    private Double humidityLowPercent;

    @Column(name = "humidity_high_percent")
    private Double humidityHighPercent;

    @Column(name = "temperature_low_celsius")
    private Double temperatureLowCelsius;

    @Column(name = "temperature_high_celsius")
    private Double temperatureHighCelsius;

    @Column(name = "pm25_warning")
    private Double pm25Warning;

    @Column(name = "pm10_warning")
    private Double pm10Warning;

    @Column(name = "strong_wind_speed")
    private Double strongWindSpeed;

    @Column(name = "stale_sensor_minutes")
    private Integer staleSensorMinutes;

    @Column(name = "rain_possible_probability")
    private Integer rainPossibleProbability;

    @Column(name = "rain_expected_probability")
    private Integer rainExpectedProbability;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public TeamIndoorPolicy(Long teamId) {
        this.teamId = teamId;
    }

    public void update(TeamIndoorPolicyOverride override) {
        this.co2WarningPpm = override.co2WarningPpm();
        this.co2DangerPpm = override.co2DangerPpm();
        this.humidityLowPercent = override.humidityLowPercent();
        this.humidityHighPercent = override.humidityHighPercent();
        this.temperatureLowCelsius = override.temperatureLowCelsius();
        this.temperatureHighCelsius = override.temperatureHighCelsius();
        this.pm25Warning = override.pm25Warning();
        this.pm10Warning = override.pm10Warning();
        this.strongWindSpeed = override.strongWindSpeed();
        this.staleSensorMinutes = override.staleSensorMinutes();
        this.rainPossibleProbability = override.rainPossibleProbability();
        this.rainExpectedProbability = override.rainExpectedProbability();
    }

    public TeamIndoorPolicyOverride toOverride() {
        return new TeamIndoorPolicyOverride(
                teamId,
                co2WarningPpm,
                co2DangerPpm,
                humidityLowPercent,
                humidityHighPercent,
                temperatureLowCelsius,
                temperatureHighCelsius,
                pm25Warning,
                pm10Warning,
                strongWindSpeed,
                staleSensorMinutes,
                rainPossibleProbability,
                rainExpectedProbability
        );
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
