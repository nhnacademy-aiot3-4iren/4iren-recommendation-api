package com.nhnacademy.recommendation.service.behavior;

import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
class BehaviorRecommendationServiceTest {

    private final BehaviorRecommendationService service = new BehaviorRecommendationService(null, null);

    @Test
    void rejectsMissingOrNonPositiveRequestValuesBeforeServing() {
        assertThatThrownBy(() -> service.recommend(null, 1L))
                .isInstanceOf(RequiredValueException.class)
                .hasMessageContaining("predictionDate");
        assertThatThrownBy(() -> service.recommend(LocalDate.of(2026, 8, 11), null))
                .isInstanceOf(RequiredValueException.class)
                .hasMessageContaining("roomId");
        assertThatThrownBy(() -> service.recommend(LocalDate.of(2026, 8, 11), 0L))
                .isInstanceOf(NotPositiveValueException.class)
                .hasMessageContaining("roomId");
    }

    @Test
    void selectsProbabilityPeaksUsingPythonDistanceSemantics() {
        double[] probabilities = {0.1, 0.9, 0.8, 0.2, 0.7, 0.1};

        assertThat(BehaviorRecommendationService.selectPeakIndices(probabilities, 2, 2))
                .containsExactly(1, 4);
    }

    @Test
    void choosesMaximumStopAfterMinimumDurationAndBeforeNextStart() {
        double[] probabilities = {0.0, 0.0, 0.3, 0.9, 0.5, 1.0};

        assertThat(BehaviorRecommendationService.chooseStopAfterStart(probabilities, 1, 5, 1))
                .isEqualTo(3);
        assertThat(BehaviorRecommendationService.chooseStopAfterStart(probabilities, 5, null, 1))
                .isNull();
    }

    @Test
    void omitsSessionWhenNoPositiveFiniteStopExists() {
        double[] zeroProbabilities = {0.4, 0.0, 0.0, 0.0};
        double[] invalidProbabilities = {0.4, Double.NaN, Double.NEGATIVE_INFINITY};

        assertThat(BehaviorRecommendationService.chooseStopAfterStart(zeroProbabilities, 0, null, 1))
                .isNull();
        assertThat(BehaviorRecommendationService.chooseStopAfterStart(invalidProbabilities, 0, null, 1))
                .isNull();
    }
}
