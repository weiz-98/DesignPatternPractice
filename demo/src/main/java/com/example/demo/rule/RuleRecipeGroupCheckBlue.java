package com.example.demo.rule;

import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.utils.ToolChamberUtil;
import com.example.demo.vo.RecipeGroupAndTool;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleRecipeGroupCheckBlue implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - Start RuleRecipeGroupCheckBlue check",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        // 1) lotType 檢查
        if (RuleUtil.isLotTypeEmpty(rule)) {
            log.info("RuncardID: {} Condition: {} - lotType is empty => skip check",
                    runcardRawInfo.getRuncardId(), cond);
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "lotType is empty => skip check");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }
        if (RuleUtil.isLotTypeMismatch(runcardRawInfo, rule)) {
            log.info("RuncardID: {} Condition: {} - lotType mismatch => skip check",
                    runcardRawInfo.getRuncardId(), cond);
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "lotType mismatch => skip check");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }

        // 2) 檢查 settings 是否存在
        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            log.info("RuncardID: {} Condition: {} - No settings => skip check",
                    runcardRawInfo.getRuncardId(), cond);
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "No settings => skip check");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }

        // 3) 找出該 cond 對應的 RecipeGroupsAndToolInfo
        List<RecipeGroupAndTool> groupsAndToolInfos = dataLoaderService.getRecipeGroupAndToolInfo(runcardRawInfo.getRuncardId());
        List<RecipeGroupAndTool> filteredGroups = groupsAndToolInfos.stream()
                .filter(rgt -> cond.equals(rgt.getCondition()))
                .toList();
        if (filteredGroups.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No RecipeGroupsAndToolInfo for condition",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = new HashMap<>();
            detail.put("error", "No RecipeGroupsAndToolInfo for condition");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }

        // 這邊按理來說只會有一筆(mapping 到指定的 condition)
        RecipeGroupAndTool rgtInfo = filteredGroups.getFirst();
        String toolIdListStr = rgtInfo.getToolIdList();  // e.g. "JDTM16,JDTM17,JDTM20"
        String recipeGroupId = rgtInfo.getRecipeGroupId();
        String recipeId = rgtInfo.getRecipeId();         // e.g. "xxx.xx-xxxx.xxxx-{cEF}{c134}"

        // 4) 將 toolIdList 解析成 List<String>
        List<String> toolIds = ToolChamberUtil.splitToolList(toolIdListStr);

        // 5) 取得 RecipeGroupCheckBlue
        List<RecipeGroupCheckBlue> checkBlueList =
                dataLoaderService.getRecipeGroupCheckBlue(recipeGroupId, toolIds);
        log.info("RuncardID: {} Condition: {} - Retrieved {} RecipeGroupCheckBlue for recipeGroupId: {} and toolIds: {}",
                runcardRawInfo.getRuncardId(), cond, checkBlueList.size(), recipeGroupId, toolIds);


        Map<String, List<List<String>>> grouped = ToolChamberUtil.parseChamberGrouped(toolIds, recipeId);
        log.info("RuncardID: {} Condition: {} - Parsed chamber grouped from recipeId: {} -> {}",
                runcardRawInfo.getRuncardId(), cond, recipeId, grouped);

        boolean pass = true;
        List<String> failTools = new ArrayList<>();

        for (Map.Entry<String, List<List<String>>> entry : grouped.entrySet()) {
            String tool = entry.getKey();
            List<List<String>> bracketExpansions = entry.getValue();  // AND

            // 對這個 tool, 逐一 bracket => "AND"
            boolean toolPass = true;
            for (List<String> orChambers : bracketExpansions) {
                // orChambers = e.g. ["E","F"]
                // 需要檢查 => "OR" => 只要找到一個 chamber 在 checkBlueList pass 即可
                boolean bracketOk = false;
                if (orChambers.isEmpty()) {
                    // 表示 {c} => "%%" or "no bracket"? => 只需檢查 tool only
                    bracketOk = checkToolOnlyPass(tool, checkBlueList);
                } else {
                    // 只要 orChambers 中任意 chamber pass => bracketOk=true
                    for (String chamber : orChambers) {
                        if (chamber.equals("%%")) {
                            if (checkToolOnlyPass(tool, checkBlueList)) {
                                bracketOk = true;
                                break;
                            }
                        } else {
                            boolean found = checkBlueList.stream().anyMatch(blue ->
                                    blue.getToolId().equals(tool)
                                            && blue.getChamberId().equals(chamber)
                                            && "1".equals(blue.getReleaseFlag())
                                            && "1".equals(blue.getEnableFlag())
                            );
                            if (found) {
                                bracketOk = true;
                                break;
                            }
                        }
                    }
                }
                if (!bracketOk) {
                    toolPass = false;
                    break;
                }
            } // end for bracketExpansions

            if (!toolPass) {
                pass = false;
                failTools.add(tool);
            }
        }

        int lamp = pass ? 1 : 3;

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("result", lamp);
        detailMap.put("recipeGroupId", recipeGroupId);
        detailMap.put("toolIdList", toolIdListStr);
        detailMap.put("recipeId", recipeId);
        detailMap.put("failTools", failTools);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue check done, lamp={}, detail={}",
                runcardRawInfo.getRuncardId(), cond, lamp, detailMap);
        return info;
    }

    /**
     * 若只指定了 tool（不含 chamber）或 chamber="%%"，表示不限定 chamber。
     * 只要在 checkBlueList 中能找到至少一筆:
     * - toolId= tool
     * - releaseFlag=1
     * - enableFlag=1
     * 即視為「通過」(true)。
     */
    private boolean checkToolOnlyPass(String tool, List<RecipeGroupCheckBlue> checkBlueList) {
        return checkBlueList.stream().anyMatch(blue ->
                blue.getToolId().equals(tool)
                        && "1".equals(blue.getReleaseFlag())
                        && "1".equals(blue.getEnableFlag())
        );
    }
}
