package com.nhnacademy.recommendation.model.behavior;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class BehaviorConfidenceLabelTest {

    @ParameterizedTest
    @MethodSource("confidenceLabels")
    void classifiesConfidenceAtExactBoundaries(double confidence, BehaviorConfidenceLabel expected) {
        assertThat(BehaviorConfidenceLabel.from(confidence)).isEqualTo(expected);
    }

    private static Stream<Arguments> confidenceLabels() {
        return Stream.of(
                Arguments.of(0.4999, BehaviorConfidenceLabel.LOW),
                Arguments.of(0.5000, BehaviorConfidenceLabel.MEDIUM),
                Arguments.of(0.5947, BehaviorConfidenceLabel.MEDIUM),
                Arguments.of(0.6335, BehaviorConfidenceLabel.MEDIUM),
                Arguments.of(0.7499, BehaviorConfidenceLabel.MEDIUM),
                Arguments.of(0.7500, BehaviorConfidenceLabel.HIGH)
        );
    }
}
