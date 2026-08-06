package com.nhnacademy.recommendation.service;


import com.nhnacademy.recommendation.dto.building.BuildingDetailResponse;
import com.nhnacademy.recommendation.dto.kma.KmaCurrentWeatherResponseDto;
import com.nhnacademy.recommendation.dto.kma.KmaForecastWeatherResponseDto;
import com.nhnacademy.recommendation.dto.room.RoomDetailResponse;
import com.nhnacademy.recommendation.dto.welcomeBriefing.CurrentWeatherSnapshot;
import com.nhnacademy.recommendation.dto.welcomeBriefing.IndoorEnvironmentPolicy;
import com.nhnacademy.recommendation.dto.welcomeBriefing.TodayWeatherOutlook;
import com.nhnacademy.recommendation.dto.welcomeBriefing.WelcomeBriefingResponse;
import com.nhnacademy.recommendation.exception.NotPositiveValueException;
import com.nhnacademy.recommendation.service.core.CoreBuildingService;
import com.nhnacademy.recommendation.service.core.CoreDeviceService;
import com.nhnacademy.recommendation.service.core.CoreRoomService;
import com.nhnacademy.recommendation.service.core.CoreWeatherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class WelcomeBriefingService {

    private final ChatClient chatClient;

    // 1. 강의실 기본 정보 조회: 강의실명, 건물 ID, 설명 등 브리핑 대상 식별에 사용
    private final CoreRoomService roomService;

    // 2. 외부 날씨 조회: 건물 위치 기준 현재 날씨와 환기 가능 여부 판단에 사용
    private final CoreWeatherService weatherService;

    // 3. 건물 위치 조회: 강의실이 속한 건물의 주소/지역명을 날씨 조회 기준으로 사용
    private final CoreBuildingService buildingService;

    // 4. 강의실 기기 조회: 조치 가능한 에어컨, 환기장치, 공기청정기 등 추천 대상 확인에 사용
    private final CoreDeviceService deviceService;

    public WelcomeBriefingService(@Qualifier("welcomeBriefingChatClient") ChatClient chatClient,
                                  CoreRoomService roomService,
                                  CoreWeatherService weatherService,
                                  CoreBuildingService buildingService,
                                  CoreDeviceService deviceService) {
        this.chatClient = chatClient;
        this.roomService = roomService;
        this.weatherService = weatherService;
        this.buildingService = buildingService;
        this.deviceService = deviceService;
    }

    // TODO: 강의실 센서 데이터 조회 서비스 연결 필요
    // - 현재 실내 센서값: 온도, 습도, CO2, 미세먼지 등 현재 상태 판단 기준
    // - 전날 시간대별 평균 센서값: 현재값과 비교해 평소보다 높은지/낮은지 판단하는 참고 기준

    public WelcomeBriefingResponse getWelcomeBriefing(Long roomId) {
        // 전체 흐름:
        // 1. 요청 파라미터 검증
        //    - roomId가 null이거나 양수가 아니면 브리핑을 만들 수 없으므로 즉시 예외 처리한다.
        if (roomId == null || roomId <= 0) {
            throw new NotPositiveValueException(roomId, "RoomID");
        }
        //
        // 2. 강의실 상세 정보 조회
        //    - roomService로 roomId에 해당하는 강의실 정보를 조회한다.
        //    - 강의실명, buildingId 등을 이후 조회의 기준 데이터로 사용한다.
        //
        RoomDetailResponse room = null;
//        roomService.getRoomDetail(roomId);
        // 3. 건물 상세 정보 조회 --> RegionName만 뽑아오면됨
        //    - buildingService로 강의실이 속한 건물 정보를 조회한다.
        //    - 건물의 regionName 또는 주소를 외부 날씨 조회 기준으로 사용한다.
        //
        String region = null;
//        roomService.getRegionName(roomId);
        // 4. 외부 날씨 조회
        //    - weatherService로 현재 날씨를 조회한다.
        //    - 환기, 창문 개방, 냉난방 조정 같은 추천 조치의 외부 조건으로 사용한다.
        //
        KmaCurrentWeatherResponseDto currentWeather = weatherService.getCurrentWeather(region);
        CurrentWeatherSnapshot currentWeatherSnapshot = currentWeatherSnapshot(currentWeather);
        // 5. 오늘 날씨 예보 조회
        //    - 웰컴 브리핑이 "지금 상태"만 설명한다면 현재 날씨만으로도 충분하다.
        //    - 하지만 "오늘 하루 동안의 강의실 관리 방법"을 안내하려면 예보도 함께 조회한다.
        //    - 예보는 비 예상, 강풍 예상, 고습 예상, 외부 온도 급변, 환기하기 좋은 시간대 판단에 사용한다.
        //    - raw 예보 목록 전체를 LLM에 넘기기보다, 코드에서 TodayWeatherOutlook으로 먼저 요약한다.
        //
        KmaForecastWeatherResponseDto forecastWeather = weatherService.getForecastWeather(region);
        // TODO: 오늘 예보 조회 및 TodayWeatherOutlook 구성 필요
        // - rainExpected: 강수확률이 정책 기준 이상이거나 강수형태/강수량이 잡혀 비가 예상되는지
        // - rainPossible: 강수확률이 낮지는 않지만 rainExpected 기준에는 못 미쳐 비 가능성이 있는지
        // - 기본 정책값은 rainPossibleProbability=30, rainExpectedProbability=60으로 시작한다.
        // - strongWindExpected: 창문 개방에 주의해야 할 정도의 강풍이 예상되는지
        // - highHumidityExpected: 제습/환기 조절이 필요할 정도의 높은 습도가 예상되는지
        // - hottestTime: 냉방 사전 점검이 필요한 최고 기온 시간대
        // - ventilationBestTime: 비/강풍/고습을 피한 환기 추천 시간대
        // - cautions: 오늘 날씨 때문에 관리자가 주의해야 할 항목
        //
        TodayWeatherOutlook todayWeatherOutlook;
        // 6. 강의실 센서 데이터 조회
        //    - 현재 센서값을 조회한다.
        //    - 전날 같은 시간대 또는 시간대별 평균 센서값을 조회한다.
        //    - 현재값은 판단 기준, 전날 평균은 비교 참고용으로만 사용한다.
        //

        // 7. 강의실 기기 목록 조회
        //    - deviceService로 강의실 내 제어/관리 가능한 기기 목록을 조회한다.
        //    - 추천 조치에 실제 기기명을 포함할 수 있도록 LLM 입력에 포함한다.
        //
        // 8. 조회 결과를 브리핑 전용 객체로 정리
        //    - 외부 API 응답 DTO를 그대로 LLM에 넘기지 않는다.
        //    - 화면/브리핑에 필요한 필드만 뽑아서 WelcomeBriefingContext 하위 객체로 재구성한다.
        //
        //    8-1. RoomInfo 구성
        //         - roomService/buildingService 조회 결과에서 roomId, roomName, buildingId, buildingName, regionName을 뽑는다.
        //         - LLM이 "어느 강의실에 대한 브리핑인지" 명확히 알 수 있게 한다.
        //
        //    8-2. CurrentIndoorSnapshot 구성
        //         - 현재 센서값에서 온도, 습도, CO2, 미세먼지 등 현재 상태 판단에 필요한 값만 뽑는다.
        //         - 센서값의 측정 시각도 함께 넣어 "현재" 데이터의 기준 시점을 명확히 한다.
        //
        //    8-3. CurrentWeatherSnapshot 구성
        //         - 현재 외부 날씨에서 외부 온도, 습도, 강수 여부, 강수량, 풍속 등을 뽑는다.
        //         - 환기/창문 개방 추천 가능 여부를 코드에서 먼저 판단할 수 있게 한다.
        //
        //    8-4. TodayWeatherOutlook 구성
        //         - 오늘 예보를 시간대별 raw 목록 그대로 넘기지 않고 하루 관리 관점으로 요약한다.
        //         - 비 예상, 비 가능성, 강풍 예상, 고습 예상, 최고 기온 시간대, 환기 추천 시간대, 주의 문구를 계산한다.
        //         - 강수확률이 조금이라도 있다고 rainExpected로 보지 않고, 정책 기준에 따라 rainPossible과 rainExpected를 분리한다.
        //         - LLM은 예보를 해석해서 새 판단을 만들지 않고, 이 요약을 근거로 자연어 브리핑을 작성한다.
        //
        //    8-5. YesterdayPatternSummary 구성
        //         - 전날 시간대별 평균 센서 데이터를 그대로 넘기지 않고 요약한다.
        //         - highCo2Hours, highHumidityHours, peakCo2Hour처럼 주의 시간대를 계산한다.
        //         - sameHourCo2Difference처럼 현재 시각과 전날 같은 시간대의 차이를 계산한다.
        //         - trend는 IMPROVED, SIMILAR, WORSENED, INSUFFICIENT_DATA 같은 enum 또는 고정 문자열로 정리한다.
        //
        //    8-6. DeviceStatus 목록 구성
        //         - 기기 목록에서 이름, 타입을 추려낸다.
        //         - 센서형 장비는 센서 데이터 흐름에서 별도로 다루므로 DeviceStatus에 포함하지 않는다.
        //         - WelcomeBriefingContext에 포함되는 기기는 조치 가능한 기기만 대상으로 한다.
        //         - 현재는 DB에 등록된 기기는 정상 작동한다고 가정하고 status를 "정상 작동"으로 표시한다.
        //         - 이후 실제 기기 상태 API가 생기면 status만 실제 상태값으로 대체한다.
        //         - LLM은 이 목록에 포함된 기기를 추천 조치에 언급할 수 있다.
        //
        //    8-7. detectedRisks 구성
        //         - CO2 높음, 습도 높음, 예보상 비/강풍으로 인한 환기 주의, 기기 비정상 같은 위험 신호를 코드에서 먼저 판단한다.
        //         - LLM은 위험을 새로 판정하기보다 detectedRisks를 근거로 자연어 브리핑을 작성한다.
        //
        //    8-8. WelcomeBriefingContext 구성
        //         - RoomInfo, CurrentIndoorSnapshot, CurrentWeatherSnapshot, TodayWeatherOutlook,
        //           YesterdayPatternSummary, DeviceStatus 목록, detectedRisks를 하나로 묶는다.
        //         - 누락된 데이터는 null 또는 빈 리스트로 명시하고, LLM이 값을 추측하지 못하도록 그대로 전달한다.
        //
        // 9. 웰컴 브리핑 ChatClient 호출
        //    - chatClient에 WelcomeBriefingContext를 JSON으로 직렬화해 전달한다.
        //    - ChatClient는 데이터를 새로 조회하지 않고, 제공된 데이터만 기반으로 요약/비교/추천을 생성한다.
        //
        // 10. 응답 파싱 및 반환
        //    - LLM 응답 JSON을 WelcomeBriefingResponse로 파싱한다.
        //    - 파싱 실패 시 fallback 응답을 줄지, 예외를 던질지 정책을 정해 처리한다.

        return null;
    }


    private CurrentWeatherSnapshot currentWeatherSnapshot(KmaCurrentWeatherResponseDto response) {
        return new CurrentWeatherSnapshot(
                response.regionName(),
                LocalDateTime.parse(response.baseDateTime()),
                response.temperature(), response.humidity(),
                response.precipitationType(),
                response.precipitationAmount(),
                response.windSpeed());
    }

    private TodayWeatherOutlook todayWeatherOutlook(KmaForecastWeatherResponseDto response){
        // TODO: IndoorEnvironmentPolicy 기준으로 rainPossible/rainExpected, 강풍, 고습, 환기 추천 시간대를 계산한다.
        return new TodayWeatherOutlook(false, false, false, false, null, null, List.of());
    }


}
