package com.example.demo.rule;

import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.utils.ToolChamberUtil;
import com.example.demo.vo.*;
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
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue check start",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        RecipeToolPair recipeToolPair = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId())
                .stream()
                .filter(o -> cond.equals(o.getCondition()))
                .findFirst()
                .map(o -> RecipeToolPair.builder()
                        .recipeId(o.getRecipeId())
                        .toolIds(o.getToolIdList())
                        .build())
                .orElseGet(() -> RecipeToolPair.builder().recipeId("").toolIds("").build());

        ResultInfo r;
        r = RuleUtil.skipIfLotTypeEmpty(cond, runcardRawInfo, rule, recipeToolPair);
        if (r != null) return r;
        r = RuleUtil.skipIfLotTypeMismatch(cond, runcardRawInfo, rule, recipeToolPair);
        if (r != null) return r;

        List<RecipeGroupAndTool> groupsAndToolInfos =
                dataLoaderService.getRecipeGroupAndTool(runcardRawInfo.getRuncardId());
        // 由於遇到 multiple recipe data 時並沒有 recipe group，需參照原 condition 的 recipe group
        List<RecipeGroupAndTool> filteredGroups = groupsAndToolInfos.stream()
                .filter(rgt -> {
                    if (cond.contains("_M")) {          // modified：如 01_M01
                        return cond.startsWith(rgt.getCondition()); // 取前半段 "01"
                    }
                    return cond.equals(rgt.getCondition());
                })
                .toList();
        if (filteredGroups.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No RecipeGroupsAndToolInfo for condition",
                    runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeToolPair, 3, "error", "No RecipeGroupsAndToolInfo for condition", false);
        }

        // 這邊按理來說只會有一筆(mapping 到指定的 condition)
        RecipeGroupAndTool rgtInfo = filteredGroups.get(0);
        String recipeGroupId = rgtInfo.getRecipeGroupId();

        String toolIdListStr = (recipeToolPair.getToolIds() == null || recipeToolPair.getToolIds().isEmpty())
                ? rgtInfo.getToolIdList()
                : recipeToolPair.getToolIds();

        List<String> toolIds = ToolChamberUtil.splitToolList(toolIdListStr);

        // 5) 取得 RecipeGroupCheckBlue
        List<RecipeGroupCheckBlue> checkBlueList =
                dataLoaderService.getRecipeGroupCheckBlue(recipeGroupId, toolIds);
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue retrieved {} for recipeGroupId: {} and toolIds: {}",
                runcardRawInfo.getRuncardId(), cond, checkBlueList.size(), recipeGroupId, toolIds);


        Map<String, List<List<String>>> grouped = ToolChamberUtil.parseChamberGrouped(toolIds, recipeToolPair.getRecipeId());
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue parsed chamber grouped from recipeId: {} -> {}",
                runcardRawInfo.getRuncardId(), cond, recipeToolPair.getRecipeId(), grouped);

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
                                            && blue.getChamberId().equals("#" + chamber)
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
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
        detailMap.put("failTools", failTools);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue detail = {}",
                runcardRawInfo.getRuncardId(), cond, detailMap);

        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue check done, lamp = '{}'",
                runcardRawInfo.getRuncardId(), cond, lamp);
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
