package com.nhnacademy.recommendation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "welcome_briefing_policy",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_welcome_briefing_policy_team_room",
                        columnNames = {"team_id", "room_id"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WelcomeBriefingPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "room_id")
    private Long roomId;

    @Column(name = "rain_possible_probability", nullable = false)
    private Integer rainPossibleProbability = 30;

    @Column(name = "rain_expected_probability", nullable = false)
    private Integer rainExpectedProbability = 60;

    @Column(name = "strong_wind_speed", nullable = false)
    private Double strongWindSpeed = 8.0;

    @Column(name = "high_humidity_percent", nullable = false)
    private Integer highHumidityPercent = 70;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public WelcomeBriefingPolicy(Long teamId,
                                 Long roomId,
                                 Integer rainPossibleProbability,
                                 Integer rainExpectedProbability,
                                 Double strongWindSpeed,
                                 Integer highHumidityPercent,
                                 Boolean enabled) {
        this.teamId = teamId;
        this.roomId = roomId;
        updatePolicy(
                rainPossibleProbability,
                rainExpectedProbability,
                strongWindSpeed,
                highHumidityPercent,
                enabled
        );
    }

    public void updatePolicy(Integer rainPossibleProbability,
                             Integer rainExpectedProbability,
                             Double strongWindSpeed,
                             Integer highHumidityPercent,
                             Boolean enabled) {
        this.rainPossibleProbability = rainPossibleProbability != null ? rainPossibleProbability : 30;
        this.rainExpectedProbability = rainExpectedProbability != null ? rainExpectedProbability : 60;
        this.strongWindSpeed = strongWindSpeed != null ? strongWindSpeed : 8.0;
        this.highHumidityPercent = highHumidityPercent != null ? highHumidityPercent : 70;
        this.enabled = enabled != null ? enabled : true;
    }

    public void updateEnabled(boolean enabled) {
        this.enabled = enabled;
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
