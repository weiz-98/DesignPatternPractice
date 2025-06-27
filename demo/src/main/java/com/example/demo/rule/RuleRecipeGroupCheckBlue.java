package com.example.demo.rule;

import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.service.BatchCache;
import com.example.demo.utils.PreCheckUtil;
import com.example.demo.utils.RuleUtil;
import com.example.demo.utils.ToolChamberUtil;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("RecipeGroupCheckBlue")
@RequiredArgsConstructor
public class RuleRecipeGroupCheckBlue implements IRuleCheck {

    @Override
    public ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule) {
        BatchCache cache = ruleExecutionContext.getCache();
        RuncardRawInfo runcardRawInfo = ruleExecutionContext.getRuncardRawInfo();
        String cond = ruleExecutionContext.getCond();
        RecipeToolPair recipeToolPair = ruleExecutionContext.getRecipeToolPair();
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue check start", runcardRawInfo.getRuncardId(), cond);

        ResultInfo pre = PreCheckUtil.run(EnumSet.of(PreCheckType.LOT_TYPE_EMPTY, PreCheckType.LOT_TYPE_MISMATCH),
                cond, runcardRawInfo, rule, recipeToolPair);
        if (pre != null) {
            return pre;
        }

        List<RecipeGroupAndTool> groupsAndToolInfos =
                cache.getRecipeGroupAndTool(runcardRawInfo.getRuncardId());
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
            log.info("RuncardID: {} Condition: {} - No RecipeGroupsAndToolInfo for condition", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No RecipeGroupsAndToolInfo for condition", false);
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
                cache.getRecipeGroupCheckBlue(recipeGroupId, toolIds);
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue retrieved {} for recipeGroupId: {} and toolIds: {}", runcardRawInfo.getRuncardId(), cond, checkBlueList.size(), recipeGroupId, toolIds);


        Map<String, List<List<String>>> grouped = ToolChamberUtil.parseChamberGrouped(toolIds, recipeToolPair.getRecipeId());
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue parsed chamber grouped from recipeId: {} -> {}", runcardRawInfo.getRuncardId(), cond, recipeToolPair.getRecipeId(), grouped);

        boolean pass = true;
        List<String> failTools = new ArrayList<>();

        for (Map.Entry<String, List<List<String>>> entry : grouped.entrySet()) {
            String tool = entry.getKey();
            boolean toolOk = isToolPass(tool, entry.getValue(), checkBlueList);
            if (!toolOk) {
                pass = false;
                failTools.add(tool);
            }
        }
        Lamp lamp = pass ? Lamp.GREEN : Lamp.RED;

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("result", lamp.code());
        detailMap.put("recipeGroupId", recipeGroupId);
        detailMap.put("toolIdList", toolIdListStr);
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
        detailMap.put("failTools", failTools);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue detail = {}", runcardRawInfo.getRuncardId(), cond, detailMap);
        log.info("RuncardID: {} Condition: {} - RecipeGroupCheckBlue check done, lamp = '{}'", runcardRawInfo.getRuncardId(), cond, lamp);
        return ResultInfo.builder()
                .ruleType(rule.getRuleType())
                .result(lamp.code())
                .detail(detailMap)
                .build();
    }

    private boolean isToolPass(String tool,
                               List<List<String>> andBrackets,
                               List<RecipeGroupCheckBlue> blueList) {

        // 每個 bracket (AND) 都必須 true
        for (List<String> orChambers : andBrackets) {
            if (!isBracketPass(tool, orChambers, blueList)) {
                return false;               // 任何一個 AND 失敗 → 整個 tool 失敗
            }
        }
        return true;
    }

    /**
     * 判斷單一 bracket (OR) 是否通過
     */
    private boolean isBracketPass(String tool,
                                  List<String> orChambers,
                                  List<RecipeGroupCheckBlue> blueList) {

        // 空 list 代表 {c} 或不限定 chamber ⇒ 只要 tool 本身 OK
        if (orChambers.isEmpty()) {
            return checkToolOnlyPass(tool, blueList);
        }

        // 只要 OR 清單裡任一 chamber 命中即可
        for (String chamber : orChambers) {
            if ("%%".equals(chamber)) {
                if (checkToolOnlyPass(tool, blueList)) {
                    return true;
                }
            } else if (blueList.stream().anyMatch(blue ->
                    blue.getToolId().equals(tool) &&
                            blue.getChamberId().equals("#" + chamber) &&
                            "1".equals(blue.getReleaseFlag()) &&
                            "1".equals(blue.getEnableFlag()))) {
                return true;
            }
        }
        return false;
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
