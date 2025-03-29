package com.example.demo.Service;


import com.example.demo.utils.ParsingUtil;
import com.example.demo.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RuncardHandlerService {

    /**
     * 建立一個 RuncardConditionMappingInfo，代表該 runcard 下的多個 condition。
     */
    public RuncardMappingInfo buildConditionMappingInfo(RuncardRawInfo runcardRawInfo, List<RecipeAndToolInfo> recipeInfos, List<ToolRuleGroup> toolRuleGroups) {


        RuncardMappingInfo checkResult = checkBasicParams(runcardRawInfo, toolRuleGroups, recipeInfos);
        if (checkResult != null) {
            return checkResult;
        }

        RuncardMappingInfo mappingInfo = new RuncardMappingInfo();
        mappingInfo.setRuncardRawInfo(runcardRawInfo);

        List<ToolRuleMappingInfo> conditionList = new ArrayList<>();

        // 每個 RecipeAndToolInfo 視為一個 "condition"
        for (RecipeAndToolInfo recipeInfo : recipeInfos) {
            // 1. 拆解 toolList
            List<String> toolList = ParsingUtil.splitToolList(recipeInfo.getToolIdList());

            // 2. 解析 recipeId => 產生多個 tool#chamber
            List<String> toolChambers = ParsingUtil.parsingChamber(toolList, recipeInfo.getRecipeId());
            log.info("RuncardID : {}, condition : {} , toolChambers: {}", runcardRawInfo.getRuncardId(), recipeInfo.getConditions(), toolChambers);

            // 3. 對這些 tool#chamber 進行 mapping
            // 同個 condition 下可能會 mapping 到多個 group，因此將 toolRuleGroup name 當作 Map 的 key，而 value 則是該 toolRuleGroup 所設定的 rule 列表
            Map<String, List<Rule>> groupRulesMap = mappingRules(toolChambers, toolRuleGroups);
            log.info("RuncardID : {}, condition : {} , groupRulesMap: {}", runcardRawInfo.getRuncardId(), recipeInfo.getConditions(), groupRulesMap);

            // 4. 組成新的 ToolRuleMappingInfo (帶 groupRulesMap)
            ToolRuleMappingInfo conditionInfo = new ToolRuleMappingInfo();

            // 不同 ToolChambers 可能來自不同資料，例如 condition 的 ToolChambers 可能從 Runcard 本身的 condition 或是從 multiple recipe 來的 ToolChambers，所以會做成 Map
            conditionInfo.setCondition(recipeInfo.getConditions());
            conditionInfo.setToolChambers(toolChambers);
            conditionInfo.setGroupRulesMap(groupRulesMap);

            conditionList.add(conditionInfo);
        }

        mappingInfo.setConditionToolRuleMappingInfos(conditionList);
        return mappingInfo;
    }


    private Map<String, List<Rule>> mappingRules(List<String> toolChambers, List<ToolRuleGroup> toolGroups) {
        Map<String, List<Rule>> resultMap = new HashMap<>();
        if (toolGroups == null || toolGroups.isEmpty() || toolChambers.isEmpty()) {
            return resultMap;
        }

        // 逐一比對 group
        for (ToolRuleGroup group : toolGroups) {
            if (group.getTools() == null || group.getTools().isEmpty()) {
                continue;
            }

            // 檢查該 group 是否有 tool#chamber 與當前 toolChambers match
            boolean groupMatched = false;
            for (String tc : toolChambers) {
                for (ToolInfo ti : group.getTools()) {
                    String fullToolChamber = ti.getToolId() + "#" + ti.getChamberId();
                    if (fullToolChamber.equals(tc)) {
                        groupMatched = true;
                        break;
                    }
                }
                if (groupMatched) break;
            }

            if (groupMatched) {
                // 代表這個 group 裡面有至少一個 tool#chamber 與當前 condition 相符
                // => 把 group.getRules() 放進 resultMap
                if (group.getGroupName() != null && group.getRules() != null) {
                    resultMap.put(group.getGroupName(), group.getRules());
                }
            }
        }

        return resultMap;
    }

    private RuncardMappingInfo checkBasicParams(
            RuncardRawInfo runcardRawInfo,
            List<ToolRuleGroup> toolRuleGroups,
            List<RecipeAndToolInfo> recipeInfos
    ) {
        if (runcardRawInfo == null) {
            log.error("[buildConditionMappingInfo] RuncardRawInfo is null, unable to proceed");
            // 直接回傳空物件，可視需求改為 throw Exception
            return new RuncardMappingInfo(null, Collections.emptyList());
        }

        if (toolRuleGroups == null || toolRuleGroups.isEmpty()) {
            log.error("[buildConditionMappingInfo] ToolGroups is null or empty, no rule mapping possible");
            // 此時至少把 RuncardRawInfo 帶回
            return new RuncardMappingInfo(runcardRawInfo, Collections.emptyList());
        }

        if (recipeInfos == null || recipeInfos.isEmpty()) {
            log.error("[buildConditionMappingInfo] RecipeAndToolInfo is null or empty, no condition to process");
            return new RuncardMappingInfo(runcardRawInfo, Collections.emptyList());
        }

        // 表示所有參數皆可用 => 回傳 null 表示無需中斷
        return null;
    }

}



