package com.example.demo.service;


import com.example.demo.utils.ToolChamberUtil;
import com.example.demo.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
public class RuncardHandlerService {

    /**
     * 建立一個 RuncardMappingInfo，代表該 runcard 下的多個 condition mapping 的結果 (包含 tool#chamber 以及 group & rules)。
     */
    public RuncardMappingInfo buildRuncardMappingInfo(RuncardRawInfo runcardRawInfo, List<OneConditionRecipeAndToolInfo> oneRuncardRecipeAndToolInfos, List<ToolRuleGroup> toolRuleGroups) {

        RuncardMappingInfo checkResult = checkBasicParams(runcardRawInfo, oneRuncardRecipeAndToolInfos);
        if (checkResult != null) {
            return checkResult;
        }

        List<ToolRuleGroup> safeGroups = (toolRuleGroups == null || toolRuleGroups.isEmpty()) ? Collections.emptyList() : toolRuleGroups;

        RuncardMappingInfo mappingInfo = new RuncardMappingInfo();
        mappingInfo.setRuncardRawInfo(runcardRawInfo);

        List<OneConditionToolRuleMappingInfo> conditionList = new ArrayList<>();

        for (OneConditionRecipeAndToolInfo oneConditionRecipeAndToolInfo : oneRuncardRecipeAndToolInfos) {
            // 解析 toolList
            List<String> toolList = ToolChamberUtil.splitToolList(oneConditionRecipeAndToolInfo.getToolIdList());
            // 解析 recipeId => 產生多個 tool#chamber
            List<String> toolChambers = ToolChamberUtil.parsingChamber(toolList, oneConditionRecipeAndToolInfo.getRecipeId());
            log.info("RuncardID: {} Condition: {} ToolChambers: {}", runcardRawInfo.getRuncardId(), oneConditionRecipeAndToolInfo.getCondition(), toolChambers);

            // 對這些 tool#chamber 進行 mapping
            // 同個 condition 下可能會 mapping 到多個 group 因此使用 Map
            Map<String, List<Rule>> groupRulesMap = mappingRules(toolChambers, safeGroups);
            log.info("RuncardID: {} Condition: {} GroupRulesMap: {}", runcardRawInfo.getRuncardId(), oneConditionRecipeAndToolInfo.getCondition(), groupRulesMap);

            OneConditionToolRuleMappingInfo oneConditionToolRuleMappingInfo = new OneConditionToolRuleMappingInfo();
            // 不同 ToolChambers 可能來自不同資料，例如 condition 的 ToolChambers 可能從 Runcard 本身的 condition 或是從 multiple recipe 來的 ToolChambers
            oneConditionToolRuleMappingInfo.setCondition(oneConditionRecipeAndToolInfo.getCondition());
            oneConditionToolRuleMappingInfo.setToolChambers(toolChambers);
            oneConditionToolRuleMappingInfo.setGroupRulesMap(groupRulesMap);

            conditionList.add(oneConditionToolRuleMappingInfo);
        }

        mappingInfo.setOneConditionToolRuleMappingInfos(conditionList);
        return mappingInfo;
    }


    public Map<String, List<Rule>> mappingRules(List<String> toolChambers, List<ToolRuleGroup> toolGroups) {
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
                // 先拆出條件端的 tool 與 chamber
                String tcTool = tc.contains("#") ? tc.substring(0, tc.indexOf('#')) : tc;
                String tcChamber = tc.contains("#") ? tc.substring(tc.indexOf('#') + 1) : "";

                for (ToolInfo ti : group.getTools()) {
                    String gTool = ti.getToolId();
                    String gChamber = ti.getChamberId() == null ? "" : ti.getChamberId().trim();

                    /* 比對規則
                     * 1) 若 group 端 chamber  為空字串 ⇒ 代表僅比對 toolId
                     * 2) 若條件端 chamber 為 "%%"   ⇒ 代表 wildcard，只需 toolId 相同
                     * 3) 其餘           ⇒ toolId 與 chamber 皆需相同
                     */
                    boolean matched;
                    if (gChamber.isEmpty()) {
                        matched = gTool.equals(tcTool);
                    } else if ("%%".equals(tcChamber)) {
                        matched = gTool.equals(tcTool);
                    } else {
                        matched = gTool.equals(tcTool) && gChamber.equals(tcChamber);
                    }

                    if (matched) {
                        groupMatched = true;
                        break;
                    }
                }
                if (groupMatched) break;
            }

            if (groupMatched && group.getGroupName() != null && group.getRules() != null) {
                resultMap.put(group.getGroupName(), group.getRules());
            }
        }
        return resultMap;
    }

    private RuncardMappingInfo checkBasicParams(
            RuncardRawInfo runcardRawInfo,
            List<OneConditionRecipeAndToolInfo> recipeInfos
    ) {
        if (runcardRawInfo == null) {
            log.error("RuncardRawInfo is null, unable to proceed");
            return new RuncardMappingInfo(null, Collections.emptyList());
        }

        if (recipeInfos == null || recipeInfos.isEmpty()) {
            log.error("RecipeAndToolInfo is null or empty, no condition to process");
            return new RuncardMappingInfo(runcardRawInfo, Collections.emptyList());
        }
        return null;
    }

}



