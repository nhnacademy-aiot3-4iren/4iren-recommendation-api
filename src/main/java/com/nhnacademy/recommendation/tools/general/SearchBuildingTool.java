package com.nhnacademy.recommendation.tools.general;

import com.nhnacademy.recommendation.adaptor.CoreClient;
import com.nhnacademy.recommendation.dto.building.BuildingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class SearchBuildingTool {

    private final CoreClient coreClient;


    @Tool(
            name = "search_building_list_by_teamId",
            description = """
                    팀 아이디로 팀이 현재 관리중인 건물리스트를 조회합니다.
                    
                    현재 세부 내용은 구현이 안된 확인용 도구입니다.
                    """
    )
    public List<BuildingResponse> getBuildingListByTeam(@ToolParam(description = "팀의 번호") Long teamId){
        log.info("[SearchBuildingTool] 팀의 관리대상 건물 목록 조회 호출");
        List<BuildingResponse> result = null;
        try{
            result = coreClient.getBuildingListByTeam(teamId).content();
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchBuildingTool]");
        }
        return result;
    }

    @Tool(
            name = "search_building_detail_by_teamId_buildingId",
            description = """
                    팀에서 관리중인 건물 중 건물번호를 통해 세부 정보를 조회합니다.
                    
                    현재는 내부 기능이 구현되지 않아 임의의 데이터를 생성하여 제공합니다.
                    테스트를 위해 유효하지 않은 데이터여도 그대로 반환하세요.
                    """
    )
    public BuildingResponse getBuildingDetail(@ToolParam(description = "팀의 번호") Long teamId,
                                              @ToolParam(description = "건물 번호") Long buildingId){
        log.info("[SearchBuildingTool] 건물 상세 정보 조회 호출 Team ID: {}, Building ID: {}",teamId, buildingId);
        try{
            return coreClient.getBuildingDetail(teamId, buildingId);
        } catch (Exception e) {
            log.info("현재 구현되지 않은 도구 호출입니다. [SearchBuildingTool]");
            return new BuildingResponse(teamId,buildingId,"오류로 발생한 건물");
        }
    }
}
