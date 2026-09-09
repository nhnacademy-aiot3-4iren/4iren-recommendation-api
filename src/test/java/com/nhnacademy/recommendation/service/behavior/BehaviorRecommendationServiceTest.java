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

    @Test
    void combinesBaseScoresWithActualActionTimingEvidence() {
        double[] base = {0.60, 0.10, 0.05};
        double[] prior = {0.0, 1.0, 0.2};

        double[] calibrated = BehaviorRecommendationService.calibrateTimingScores(base, prior, 0.4);

        assertThat(calibrated).containsExactly(0.36, 0.46, 0.11000000000000001);
        assertThat(calibrated[1]).isGreaterThan(base[1]);
    }

    @Test
    void zeroTimingWeightPreservesBaseScoresExactly() {
        double[] base = {0.123456789, 0.987654321};
        double[] prior = {1.0, 0.0};

        assertThat(BehaviorRecommendationService.calibrateTimingScores(base, prior, 0.0))
                .containsExactly(base);
    }

    @Test
    void actualActionTimingErrorDoesNotWorsenAfterCalibration() {
        double[] baseStop = new double[48];
        double[] stopPrior = new double[48];
        baseStop[46] = 0.6111; // 23:00 baseline
        baseStop[39] = 0.00195; // actual history around 19:19
        stopPrior[39] = 1.0;

        double[] calibrated = BehaviorRecommendationService.calibrateTimingScores(
                baseStop, stopPrior, 0.48
        );
        int before = BehaviorRecommendationService.selectPeakIndices(baseStop, 1, 2).getFirst();
        int after = BehaviorRecommendationService.selectPeakIndices(calibrated, 1, 2).getFirst();

        assertThat(Math.abs(after * 30 - 19 * 60 - 19))
                .isLessThanOrEqualTo(Math.abs(before * 30 - 19 * 60 - 19));
        assertThat(after).isEqualTo(39);
    }
}
