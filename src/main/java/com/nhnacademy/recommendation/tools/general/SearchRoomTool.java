package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.room.RoomResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchRoomTool {
    private final CoreClient coreClient;

    @Tool(
            name = "search_room_list_by_buildingId",
            description = """
                    Team ID와 Building ID를 통해 건물 내 강의실 리스트를 조회합니다.
                    """
    )
    public List<RoomResponse> getRoomListByBuilding(@ToolParam(description = "팀의 번호") Long teamId, @ToolParam(description = "건물 번호") Long buildingId){
        log.info("[SearchRoomTool] 건물 내 강의실 목록 조회 호출 TeamID: {}, BuildingID: {}", teamId, buildingId);

        List<RoomResponse> result = null;
        try{
            result = coreClient.getRoomListByBuilding(teamId, buildingId).content();
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchRoomTool]");
        }
        return result;
    }

    @Tool(
            name = "search_room_detail_by_roomId",
            description = """
                    TeamId와 RoomId를 통해 해당하는 강의실 세부 정보를 조회합니다.
                    """
    )
    public RoomResponse getRoomDetail(@ToolParam(description = "팀의 번호")Long teamId, @ToolParam(description = "강의실 번호") Long roomId){
        log.info("[SearchRoomTool] 강의실 상세 정보 조회 호출 TeamID: {}, RoomID: {}", teamId, roomId);
        try{
            return coreClient.getRoomDetail(teamId, roomId);
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchBuildingTool]");
            return new RoomResponse(roomId,1L,"오류로 발생한 강의실");
        }
    }
}
