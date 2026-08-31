package com.nhnacademy.recommendation.service.behavior;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.nhnacademy.recommendation.exception.BundleValidationException;
import com.nhnacademy.recommendation.exception.ModelServingException;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.exception.RequiredValueException;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendation;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.DeviceUsageDecision;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.HvacSchedule;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.Session;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.SessionProfile;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.VentilationEvent;
import com.nhnacademy.recommendation.model.behavior.BehaviorRecommendationResult.VentilationSchedule;
import com.nhnacademy.recommendation.model.serving.ModelServingInfrastructure;
import com.nhnacademy.recommendation.model.serving.RuntimeCsvTable;
import com.nhnacademy.recommendation.model.serving.SpringServingContract;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

@Slf4j
@Service
@ConditionalOnProperty(prefix = "model.serving", name = "enabled", havingValue = "true", matchIfMissing = true)
public class BehaviorRecommendationService {

    private static final String AIR_CONDITIONER = "AIR_CONDITIONER";
    private static final String HEATER = "HEATER";
    private static final String VENTILATION = "VENTILATION";
    private static final String ON = "ON";
    private static final String OFF = "OFF";
    private static final String REGIME_SOURCE = "HISTORICAL_DAILY_OUTSIDE_TEMPERATURE_CLUSTER";
    private static final String DAILY_USAGE_PREFIX = "behavior.dailyUsage.";
    private static final String EVENT_PREFIX = "behavior.events.";
    private static final List<String> DEVICE_TYPES = List.of(AIR_CONDITIONER, HEATER);
    private static final List<String> EVENT_TYPES = List.of(
            "AIR_CONDITIONER_START",
            "AIR_CONDITIONER_STOP",
            "HEATER_START",
            "HEATER_STOP",
            "VENTILATION_EVENT"
    );
    private static final DateTimeFormatter TIME_TEXT = DateTimeFormatter.ofPattern("HH:mm");

    private final ModelServingInfrastructure infrastructure;
    private final OrtEnvironment environment;
    private volatile RuntimeData runtimeData;

    public BehaviorRecommendationService(ModelServingInfrastructure infrastructure,
                                         OrtEnvironment environment) {
        this.infrastructure = infrastructure;
        this.environment = environment;
    }

    public BehaviorRecommendation recommend(LocalDate predictionDate, Long roomId) {
        return recommendWithDiagnostics(predictionDate, roomId).recommendation();
    }

    public BehaviorRecommendationResult recommendWithDiagnostics(LocalDate predictionDate, Long roomId) {
        validateRequest(predictionDate, roomId);

        String location = infrastructure.getRoomPreference(roomId).location();
        SpringServingContract contract = infrastructure.contract();
        SpringServingContract.BehaviorOrchestrationSpec behavior = contract.behaviorOrchestration();
        ZoneId zoneId = requiredZoneId(behavior.timezone());
        RuntimeData data = runtimeData(zoneId);

        RegimeInference regime = inferTemperatureRegime(
                predictionDate,
                data.regimeHistory(),
                behavior.dayOfYearCycle()
        );
        Map<String, Object> dailyFeature = dailyFeature(predictionDate, location, regime.regime(), behavior);
        Map<String, DeviceUsageDecision> decisions = predictDailyUsage(dailyFeature, regime.regime(), contract);

        List<Map<String, Object>> eventFeatures = eventFeatures(
                predictionDate,
                location,
                regime.regime(),
                contract.behaviorBinMinutes(),
                behavior
        );
        Map<String, double[]> eventProbabilities = predictEvents(eventFeatures, contract);

        ComparableHistory comparable = comparableHistory(
                predictionDate,
                location,
                regime.regime(),
                data.eventHistory(),
                behavior.sameWeekdayMinimumHistoricalDates()
        );

        HvacBuild ac = buildHvac(
                AIR_CONDITIONER,
                decisions.get(AIR_CONDITIONER),
                comparable,
                eventProbabilities,
                contract.behaviorBinMinutes(),
                behavior
        );
        HvacBuild heater = buildHvac(
                HEATER,
                decisions.get(HEATER),
                comparable,
                eventProbabilities,
                contract.behaviorBinMinutes(),
                behavior
        );
        VentilationBuild ventilation = buildVentilation(
                comparable,
                eventProbabilities.get("VENTILATION_EVENT"),
                contract.behaviorBinMinutes(),
                behavior
        );

        List<ScheduleRow> scheduleRows = new ArrayList<>();
        scheduleRows.addAll(ac.rows());
        scheduleRows.addAll(heater.rows());
        scheduleRows.addAll(ventilation.rows());
        scheduleRows.sort(Comparator
                .comparingInt(ScheduleRow::sortMinute)
                .thenComparingInt(row -> OFF.equals(row.item().action()) ? 0 : 1)
                .thenComparing(row -> row.item().deviceType()));

        BehaviorRecommendation recommendation = new BehaviorRecommendation(
                behavior.schemaVersion(),
                new BehaviorRecommendation.Context(
                        predictionDate,
                        predictionDate.getDayOfWeek(),
                        roomId,
                        location,
                        behavior.timezone()
                ),
                behavior.recommendationType(),
                scheduleRows.stream().map(ScheduleRow::item).toList()
        );

        Map<String, Object> eventSchedule = new LinkedHashMap<>();
        eventSchedule.put(AIR_CONDITIONER, ac.diagnostics());
        eventSchedule.put(HEATER, heater.diagnostics());
        eventSchedule.put(VENTILATION, ventilation.diagnostics());
        log.debug("[Behavior] recommendation 생성 완료. date={}, roomId={}, location={}, regime={}, source={}",
                predictionDate, roomId, location, regime.regime(), REGIME_SOURCE);
        return new BehaviorRecommendationResult(
                recommendation,
                regime.regime(),
                decisions,
                eventSchedule
        );
    }

    private void validateRequest(LocalDate predictionDate, Long roomId) {
        if (predictionDate == null) {
            throw new RequiredValueException("predictionDate");
        }
        if (roomId == null) {
            throw new RequiredValueException("roomId");
        }
        if (roomId <= 0) {
            throw new NotPositiveValueException(roomId, "roomId");
        }
    }

    private RuntimeData runtimeData(ZoneId zoneId) {
        RuntimeData loaded = runtimeData;
        if (loaded != null) {
            return loaded;
        }
        synchronized (this) {
            if (runtimeData == null) {
                runtimeData = new RuntimeData(
                        loadRegimeHistory(infrastructure.runtimeArtifacts().csv("behaviorTemperatureRegimeHistory")),
                        loadEventHistory(infrastructure.runtimeArtifacts().csv("behaviorEventHistory"), zoneId)
                );
            }
            return runtimeData;
        }
    }

    private List<RegimeHistoryRow> loadRegimeHistory(RuntimeCsvTable table) {
        int dateIndex = table.columnIndex("local_date");
        int regimeIndex = table.columnIndex("temperature_regime");
        int dayOfYearIndex = table.columnIndex("day_of_year");
        List<RegimeHistoryRow> result = new ArrayList<>(table.rows().size());
        for (int index = 0; index < table.rows().size(); index++) {
            List<String> row = table.rows().get(index);
            try {
                result.add(new RegimeHistoryRow(
                        LocalDate.parse(row.get(dateIndex)),
                        row.get(regimeIndex),
                        Integer.parseInt(row.get(dayOfYearIndex))
                ));
            } catch (DateTimeParseException | NumberFormatException exception) {
                throw malformedHistory("behavior_temperature_regime_history.csv", index, exception);
            }
        }
        return List.copyOf(result);
    }

    private List<EventHistoryRow> loadEventHistory(RuntimeCsvTable table, ZoneId zoneId) {
        int locationIndex = table.columnIndex("location");
        int timeIndex = table.columnIndex("time_bin");
        int regimeIndex = table.columnIndex("temperature_regime");
        int acStartIndex = table.columnIndex("air_conditioner_start_event");
        int acStopIndex = table.columnIndex("air_conditioner_stop_event");
        int heaterStartIndex = table.columnIndex("heater_start_event");
        int heaterStopIndex = table.columnIndex("heater_stop_event");
        int ventilationIndex = table.columnIndex("ventilation_event");
        List<EventHistoryRow> result = new ArrayList<>(table.rows().size());
        for (int index = 0; index < table.rows().size(); index++) {
            List<String> row = table.rows().get(index);
            try {
                OffsetDateTime timestamp = OffsetDateTime.parse(row.get(timeIndex).replace(' ', 'T'));
                LocalDate localDate = timestamp.atZoneSameInstant(zoneId).toLocalDate();
                result.add(new EventHistoryRow(
                        row.get(locationIndex),
                        localDate,
                        localDate.getDayOfWeek(),
                        row.get(regimeIndex),
                        Integer.parseInt(row.get(acStartIndex)),
                        Integer.parseInt(row.get(acStopIndex)),
                        Integer.parseInt(row.get(heaterStartIndex)),
                        Integer.parseInt(row.get(heaterStopIndex)),
                        Integer.parseInt(row.get(ventilationIndex))
                ));
            } catch (DateTimeParseException | NumberFormatException exception) {
                throw malformedHistory("behavior_event_training_30m.csv", index, exception);
            }
        }
        return List.copyOf(result);
    }

    private BundleValidationException malformedHistory(String filename, int zeroBasedRow, Exception cause) {
        return new BundleValidationException(
                "Behavior runtime history 행을 해석할 수 없습니다: " + filename + " line=" + (zeroBasedRow + 2),
                cause
        );
    }

    private RegimeInference inferTemperatureRegime(LocalDate predictionDate,
                                                   List<RegimeHistoryRow> history,
                                                   double dayOfYearCycle) {
        List<RegimeHistoryRow> comparable = history.stream()
                .filter(row -> row.localDate().isBefore(predictionDate))
                .toList();
        if (comparable.isEmpty()) {
            comparable = history.stream()
                    .filter(row -> !row.localDate().equals(predictionDate))
                    .toList();
        }
        if (comparable.isEmpty()) {
            throw new ModelServingException("predictionDate의 Temperature Regime을 추론할 과거 군집 이력이 없습니다.");
        }

        Map<String, Double> scores = new LinkedHashMap<>();
        for (RegimeHistoryRow row : comparable) {
            double difference = Math.abs(row.dayOfYear() - predictionDate.getDayOfYear());
            double dayDistance = Math.min(difference, dayOfYearCycle - difference);
            scores.merge(row.temperatureRegime(), 1.0 / (1.0 + dayDistance), Double::sum);
        }
        String selected = null;
        double selectedScore = Double.NEGATIVE_INFINITY;
        for (Map.Entry<String, Double> entry : scores.entrySet()) {
            if (entry.getValue() > selectedScore) {
                selected = entry.getKey();
                selectedScore = entry.getValue();
            }
        }
        return new RegimeInference(selected);
    }

    private Map<String, Object> dailyFeature(LocalDate date,
                                             String location,
                                             String regime,
                                             SpringServingContract.BehaviorOrchestrationSpec behavior) {
        int weekday = date.getDayOfWeek().getValue() - 1;
        int dayOfYear = date.getDayOfYear();
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("location", location);
        feature.put("temperature_regime", regime);
        feature.put("weekday_sin", Math.sin(2.0 * Math.PI * weekday / behavior.weekdayCycle()));
        feature.put("weekday_cos", Math.cos(2.0 * Math.PI * weekday / behavior.weekdayCycle()));
        feature.put("day_of_year_sin", Math.sin(2.0 * Math.PI * dayOfYear / behavior.dayOfYearCycle()));
        feature.put("day_of_year_cos", Math.cos(2.0 * Math.PI * dayOfYear / behavior.dayOfYearCycle()));
        return feature;
    }

    private Map<String, DeviceUsageDecision> predictDailyUsage(Map<String, Object> feature,
                                                               String regime,
                                                               SpringServingContract contract) {
        Map<String, DeviceUsageDecision> result = new LinkedHashMap<>();
        SpringServingContract.BehaviorOrchestrationSpec behavior = contract.behaviorOrchestration();
        for (String deviceType : DEVICE_TYPES) {
            SpringServingContract.OnnxModelSpec spec = requiredProbabilityModel(
                    contract,
                    DAILY_USAGE_PREFIX + deviceType,
                    true
            );
            double probability = predictPositiveProbability(spec, List.of(feature))[0];
            double threshold = spec.threshold();
            Set<String> allowedRegimes = behavior.regimeGating().get(deviceType);
            if (allowedRegimes == null) {
                throw new BundleValidationException("contract regime gating이 없습니다: " + deviceType);
            }
            boolean allowed = allowedRegimes.contains(regime);
            result.put(deviceType, new DeviceUsageDecision(
                    allowed && probability >= threshold,
                    round(probability, behavior.confidenceRoundingDecimals()),
                    round(threshold, behavior.confidenceRoundingDecimals()),
                    allowed
            ));
        }
        return Map.copyOf(result);
    }

    private List<Map<String, Object>> eventFeatures(LocalDate date,
                                                    String location,
                                                    String regime,
                                                    int binMinutes,
                                                    SpringServingContract.BehaviorOrchestrationSpec behavior) {
        int weekday = date.getDayOfWeek().getValue() - 1;
        int dayOfYear = date.getDayOfYear();
        List<Map<String, Object>> rows = new ArrayList<>(1440 / binMinutes);
        for (int minute = 0; minute < 1440; minute += binMinutes) {
            double hour = minute / 60.0;
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("location", location);
            row.put("temperature_regime", regime);
            row.put("hour_sin", Math.sin(2.0 * Math.PI * hour / behavior.hourCycle()));
            row.put("hour_cos", Math.cos(2.0 * Math.PI * hour / behavior.hourCycle()));
            row.put("weekday_sin", Math.sin(2.0 * Math.PI * weekday / behavior.weekdayCycle()));
            row.put("weekday_cos", Math.cos(2.0 * Math.PI * weekday / behavior.weekdayCycle()));
            row.put("day_of_year_sin", Math.sin(2.0 * Math.PI * dayOfYear / behavior.dayOfYearCycle()));
            row.put("day_of_year_cos", Math.cos(2.0 * Math.PI * dayOfYear / behavior.dayOfYearCycle()));
            rows.add(row);
        }
        return List.copyOf(rows);
    }

    private Map<String, double[]> predictEvents(List<Map<String, Object>> features,
                                                SpringServingContract contract) {
        Map<String, double[]> result = new LinkedHashMap<>();
        for (String eventType : EVENT_TYPES) {
            SpringServingContract.OnnxModelSpec spec = requiredProbabilityModel(
                    contract,
                    EVENT_PREFIX + eventType,
                    false
            );
            result.put(eventType, predictPositiveProbability(spec, features));
        }
        return Map.copyOf(result);
    }

    private SpringServingContract.OnnxModelSpec requiredProbabilityModel(SpringServingContract contract,
                                                                         String modelKey,
                                                                         boolean thresholdRequired) {
        SpringServingContract.OnnxModelSpec spec = contract.models().get(modelKey);
        if (spec == null) {
            throw new BundleValidationException("contract Behavior 모델이 없습니다: " + modelKey);
        }
        if (spec.outputName() == null || !spec.outputs().contains(spec.outputName())) {
            throw new BundleValidationException("contract probability outputName이 유효하지 않습니다: " + modelKey);
        }
        if (spec.positiveProbabilityColumn() == null || spec.positiveProbabilityColumn() < 0) {
            throw new BundleValidationException("contract positiveProbabilityColumn이 유효하지 않습니다: " + modelKey);
        }
        if (thresholdRequired && spec.threshold() == null) {
            throw new BundleValidationException("contract Daily Usage threshold가 없습니다: " + modelKey);
        }
        return spec;
    }

    private double[] predictPositiveProbability(SpringServingContract.OnnxModelSpec spec,
                                                List<Map<String, Object>> rows) {
        Map<String, OnnxTensor> inputs = new LinkedHashMap<>();
        try {
            for (SpringServingContract.OnnxInputSpec input : spec.inputs()) {
                inputs.put(input.name(), createTensor(input, rows));
            }
            OrtSession session = infrastructure.sessions().getRequired(spec.key());
            try (OrtSession.Result inference = session.run(inputs)) {
                Object value = inference.get(spec.outputName())
                        .orElseThrow(() -> new BundleValidationException(
                                "ONNX probability output이 없습니다: " + spec.key() + ":" + spec.outputName()
                        ))
                        .getValue();
                if (!(value instanceof float[][] probabilities)) {
                    throw new ModelServingException("ONNX probability output shape이 [N,C] float32가 아닙니다: "
                            + spec.key());
                }
                double[] positive = new double[probabilities.length];
                for (int row = 0; row < probabilities.length; row++) {
                    int column = spec.positiveProbabilityColumn();
                    if (column >= probabilities[row].length) {
                        throw new ModelServingException("ONNX positive probability column이 output 범위를 벗어납니다: "
                                + spec.key());
                    }
                    positive[row] = probabilities[row][column];
                }
                return positive;
            }
        } catch (ModelServingException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ModelServingException("Behavior ONNX inference에 실패했습니다: " + spec.key(), exception);
        } finally {
            inputs.values().forEach(tensor -> {
                try {
                    tensor.close();
                } catch (Exception exception) {
                    log.warn("[Behavior] input tensor 종료에 실패했습니다. model={}", spec.key(), exception);
                }
            });
        }
    }

    private OnnxTensor createTensor(SpringServingContract.OnnxInputSpec input,
                                    List<Map<String, Object>> rows) throws Exception {
        if ("tensor(string)".equals(input.dtype())) {
            String[][] values = new String[rows.size()][1];
            for (int row = 0; row < rows.size(); row++) {
                Object value = requiredFeature(rows.get(row), input.name());
                values[row][0] = value.toString();
            }
            return OnnxTensor.createTensor(environment, values);
        }
        if ("tensor(float)".equals(input.dtype())) {
            float[][] values = new float[rows.size()][1];
            for (int row = 0; row < rows.size(); row++) {
                Object value = requiredFeature(rows.get(row), input.name());
                if (!(value instanceof Number number)) {
                    throw new BundleValidationException("Behavior numeric feature가 숫자가 아닙니다: " + input.name());
                }
                values[row][0] = number.floatValue();
            }
            return OnnxTensor.createTensor(environment, values);
        }
        throw new BundleValidationException("Behavior ONNX input dtype을 지원하지 않습니다: " + input.dtype());
    }

    private Object requiredFeature(Map<String, Object> row, String name) {
        Object value = row.get(name);
        if (value == null) {
            throw new BundleValidationException("Behavior ONNX 필수 feature가 없습니다: " + name);
        }
        return value;
    }

    private ComparableHistory comparableHistory(LocalDate predictionDate,
                                                String location,
                                                String regime,
                                                List<EventHistoryRow> history,
                                                int sameWeekdayMinimumDates) {
        List<EventHistoryRow> comparable = history.stream()
                .filter(row -> row.location().equals(location))
                .filter(row -> row.temperatureRegime().equals(regime))
                .filter(row -> row.localDate().isBefore(predictionDate))
                .toList();
        if (comparable.isEmpty()) {
            return new ComparableHistory(List.of(), false);
        }
        List<EventHistoryRow> sameWeekday = comparable.stream()
                .filter(row -> row.weekday() == predictionDate.getDayOfWeek())
                .toList();
        long historicalDates = sameWeekday.stream().map(EventHistoryRow::localDate).distinct().count();
        return historicalDates >= sameWeekdayMinimumDates
                ? new ComparableHistory(sameWeekday, true)
                : new ComparableHistory(comparable, false);
    }

    private SessionEstimate estimateSessions(ComparableHistory comparable,
                                             ToIntFunction<EventHistoryRow> startValue,
                                             int decimals) {
        if (comparable.rows().isEmpty()) {
            return new SessionEstimate(
                    1,
                    new SessionProfile(0, 0, 0.0, 1, comparable.usedSameWeekday(),
                            "DAILY_USAGE_YES_NO_HISTORY")
            );
        }
        Map<LocalDate, Integer> dailyCounts = sumByDate(comparable.rows(), startValue);
        List<Integer> activeCounts = dailyCounts.values().stream().filter(count -> count > 0).toList();
        double meanActive = activeCounts.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int recommended = activeCounts.isEmpty() ? 1 : Math.max(1, roundHalfUpToInt(meanActive));
        return new SessionEstimate(
                recommended,
                new SessionProfile(
                        dailyCounts.size(),
                        activeCounts.size(),
                        round(meanActive, decimals),
                        recommended,
                        comparable.usedSameWeekday(),
                        null
                )
        );
    }

    private HvacBuild buildHvac(String deviceType,
                                DeviceUsageDecision decision,
                                ComparableHistory comparable,
                                Map<String, double[]> probabilities,
                                int binMinutes,
                                SpringServingContract.BehaviorOrchestrationSpec behavior) {
        if (!decision.useToday()) {
            return new HvacBuild(
                    List.of(),
                    new HvacSchedule(deviceType, false, decision.probability(), decision.threshold(),
                            0, null, null)
            );
        }

        String startEvent;
        String stopEvent;
        ToIntFunction<EventHistoryRow> startValue;
        if (AIR_CONDITIONER.equals(deviceType)) {
            startEvent = "AIR_CONDITIONER_START";
            stopEvent = "AIR_CONDITIONER_STOP";
            startValue = EventHistoryRow::airConditionerStart;
        } else if (HEATER.equals(deviceType)) {
            startEvent = "HEATER_START";
            stopEvent = "HEATER_STOP";
            startValue = EventHistoryRow::heaterStart;
        } else {
            throw new IllegalArgumentException(deviceType);
        }

        SessionEstimate estimate = estimateSessions(
                comparable,
                startValue,
                behavior.confidenceRoundingDecimals()
        );
        List<Integer> startIndices = selectPeakIndices(
                probabilities.get(startEvent),
                estimate.count(),
                behavior.hvacMinimumDistanceBins()
        );
        List<ScheduleRow> rows = new ArrayList<>();
        List<Session> sessions = new ArrayList<>();
        for (int position = 0; position < startIndices.size(); position++) {
            int startIndex = startIndices.get(position);
            Integer nextStartIndex = position + 1 < startIndices.size() ? startIndices.get(position + 1) : null;
            Integer stopIndex = chooseStopAfterStart(
                    probabilities.get(stopEvent),
                    startIndex,
                    nextStartIndex,
                    behavior.minimumStopAfterStartBins()
            );
            if (stopIndex == null) {
                continue;
            }
            int startMinute = startIndex * binMinutes;
            int stopMinute = stopIndex * binMinutes;
            LocalTime startTime = localTime(startMinute);
            LocalTime stopTime = localTime(stopMinute);
            double startProbability = probabilities.get(startEvent)[startIndex];
            double stopProbability = probabilities.get(stopEvent)[stopIndex];
            double confidence = round(
                    (startProbability + stopProbability) / 2.0,
                    behavior.confidenceRoundingDecimals()
            );
            rows.add(new ScheduleRow(
                    startMinute,
                    new BehaviorRecommendation.ScheduleItem(deviceType, ON, startTime, stopTime, confidence)
            ));
            rows.add(new ScheduleRow(
                    stopMinute,
                    new BehaviorRecommendation.ScheduleItem(
                            deviceType,
                            OFF,
                            stopTime,
                            null,
                            round(stopProbability, behavior.confidenceRoundingDecimals())
                    )
            ));
            sessions.add(new Session(
                    startTime.format(TIME_TEXT),
                    round(startProbability, behavior.confidenceRoundingDecimals()),
                    stopTime.format(TIME_TEXT),
                    round(stopProbability, behavior.confidenceRoundingDecimals())
            ));
        }
        return new HvacBuild(
                List.copyOf(rows),
                new HvacSchedule(
                        deviceType,
                        true,
                        decision.probability(),
                        decision.threshold(),
                        sessions.size(),
                        estimate.profile(),
                        sessions
                )
        );
    }

    static List<Integer> selectPeakIndices(double[] probability, int count, int minimumDistanceBins) {
        if (count <= 0 || probability.length == 0) {
            return List.of();
        }
        List<Integer> indices = new ArrayList<>(probability.length);
        for (int index = 0; index < probability.length; index++) {
            indices.add(index);
        }
        indices.sort((left, right) -> {
            int probabilityOrder = Double.compare(probability[right], probability[left]);
            return probabilityOrder != 0 ? probabilityOrder : Integer.compare(right, left);
        });

        List<Integer> selected = new ArrayList<>();
        for (Integer index : indices) {
            if (!Double.isFinite(probability[index])) {
                continue;
            }
            boolean tooClose = selected.stream()
                    .anyMatch(chosen -> Math.abs(index - chosen) < minimumDistanceBins);
            if (tooClose) {
                continue;
            }
            selected.add(index);
            if (selected.size() >= count) {
                break;
            }
        }
        selected.sort(Integer::compareTo);
        return List.copyOf(selected);
    }

    static Integer chooseStopAfterStart(double[] stopProbability,
                                        int startIndex,
                                        Integer nextStartIndex,
                                        int minimumStopAfterStartBins) {
        int firstStop = startIndex + minimumStopAfterStartBins;
        int lastExclusive = nextStartIndex == null ? stopProbability.length : nextStartIndex;
        if (firstStop >= lastExclusive) {
            return null;
        }
        Integer selected = null;
        for (int index = firstStop; index < lastExclusive; index++) {
            if (!Double.isFinite(stopProbability[index])) {
                continue;
            }
            if (selected == null || stopProbability[index] > stopProbability[selected]) {
                selected = index;
            }
        }
        return selected == null || stopProbability[selected] <= 0.0 ? null : selected;
    }

    private VentilationBuild buildVentilation(ComparableHistory comparable,
                                              double[] probability,
                                              int binMinutes,
                                              SpringServingContract.BehaviorOrchestrationSpec behavior) {
        if (comparable.rows().isEmpty()) {
            return new VentilationBuild(List.of(), new VentilationSchedule(0, null, null, null));
        }
        Map<LocalDate, Integer> dailyCounts = sumByDate(comparable.rows(), EventHistoryRow::ventilation);
        double meanCount = dailyCounts.values().stream().mapToInt(Integer::intValue).average().orElse(0.0);
        int eventCount = roundHalfUpToInt(meanCount);
        if (eventCount <= 0) {
            return new VentilationBuild(
                    List.of(),
                    new VentilationSchedule(
                            0,
                            round(meanCount, behavior.confidenceRoundingDecimals()),
                            comparable.usedSameWeekday(),
                            null
                    )
            );
        }
        List<Integer> indices = selectPeakIndices(
                probability,
                eventCount,
                behavior.ventilationMinimumDistanceBins()
        );
        List<ScheduleRow> rows = new ArrayList<>();
        List<VentilationEvent> events = new ArrayList<>();
        for (Integer index : indices) {
            int startMinute = index * binMinutes;
            LocalTime startTime = localTime(startMinute);
            LocalTime endTime = startTime.plusMinutes(behavior.ventilationDurationMinutes());
            double confidence = round(probability[index], behavior.confidenceRoundingDecimals());
            rows.add(new ScheduleRow(
                    startMinute,
                    new BehaviorRecommendation.ScheduleItem(VENTILATION, ON, startTime, endTime, confidence)
            ));
            events.add(new VentilationEvent(startTime.format(TIME_TEXT), confidence));
        }
        return new VentilationBuild(
                List.copyOf(rows),
                new VentilationSchedule(
                        events.size(),
                        round(meanCount, behavior.confidenceRoundingDecimals()),
                        comparable.usedSameWeekday(),
                        events
                )
        );
    }

    private Map<LocalDate, Integer> sumByDate(List<EventHistoryRow> rows,
                                              ToIntFunction<EventHistoryRow> value) {
        Map<LocalDate, Integer> counts = new HashMap<>();
        for (EventHistoryRow row : rows) {
            counts.merge(row.localDate(), value.applyAsInt(row), Integer::sum);
        }
        return counts;
    }

    private int roundHalfUpToInt(double value) {
        return (int) Math.floor(value + 0.5);
    }

    private double round(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_EVEN).doubleValue();
    }

    private LocalTime localTime(int minuteOfDay) {
        return LocalTime.of((minuteOfDay / 60) % 24, minuteOfDay % 60);
    }

    private ZoneId requiredZoneId(String timezone) {
        try {
            return ZoneId.of(timezone);
        } catch (Exception exception) {
            throw new BundleValidationException("contract Behavior timezone이 유효하지 않습니다: " + timezone,
                    exception);
        }
    }

    private record RuntimeData(
            List<RegimeHistoryRow> regimeHistory,
            List<EventHistoryRow> eventHistory
    ) {
    }

    private record RegimeHistoryRow(LocalDate localDate, String temperatureRegime, int dayOfYear) {
    }

    private record EventHistoryRow(
            String location,
            LocalDate localDate,
            DayOfWeek weekday,
            String temperatureRegime,
            int airConditionerStart,
            int airConditionerStop,
            int heaterStart,
            int heaterStop,
            int ventilation
    ) {
    }

    private record RegimeInference(String regime) {
    }

    private record ComparableHistory(List<EventHistoryRow> rows, boolean usedSameWeekday) {
    }

    private record SessionEstimate(int count, SessionProfile profile) {
    }

    private record ScheduleRow(int sortMinute, BehaviorRecommendation.ScheduleItem item) {
    }

    private record HvacBuild(List<ScheduleRow> rows, HvacSchedule diagnostics) {
    }

    private record VentilationBuild(List<ScheduleRow> rows, VentilationSchedule diagnostics) {
    }
}
