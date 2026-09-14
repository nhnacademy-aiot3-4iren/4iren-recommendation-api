package com.nhnacademy.recommendation.model.behavior;

public enum BehaviorConfidenceLabel {
    LOW,
    MEDIUM,
    HIGH;

    private static final double MEDIUM_THRESHOLD = 0.50;
    private static final double HIGH_THRESHOLD = 0.75;

    public static BehaviorConfidenceLabel from(double confidence) {
        if (confidence < MEDIUM_THRESHOLD) {
            return LOW;
        }
        if (confidence < HIGH_THRESHOLD) {
            return MEDIUM;
        }
        return HIGH;
    }
}
