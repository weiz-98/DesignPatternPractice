package com.example.demo.rule;

import com.example.demo.po.ForwardProcess;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
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
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleForwardProcess implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - Start RuleForwardProcess check",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        if (RuleUtil.isLotTypeEmpty(rule)) {
            log.info("RuncardID: {} Condition: {} - lotType is empty => skip check",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(0);
            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "lotType is empty => skip check");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
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
            info.setDetail(detail);
            return info;
        }

        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            log.info("RuncardID: {} Condition: {} - No settings => skip check",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(0);
            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "No settings => skip check");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            info.setDetail(detail);
            return info;
        }

        int forwardSteps = RuleUtil.parseIntSafe(settings.get("forwardSteps"));
        boolean includeMeasurement = RuleUtil.parseBooleanSafe(settings.get("includeMeasurement"));
        List<String> recipeIds = RuleUtil.parseStringList(settings.get("recipeIds"));
        List<String> toolIds = RuleUtil.parseStringList(settings.get("toolIds"));

        log.info("RuncardID: {} Condition: {} - forwardSteps={}, includeMeasurement={}, recipeIds={}, toolIds={}",
                runcardRawInfo.getRuncardId(), cond, forwardSteps, includeMeasurement, recipeIds, toolIds);

        List<ForwardProcess> allForward = dataLoaderService.getForwardProcess();
        if (allForward.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No ForwardProcess data => skip",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = new HashMap<>();
            detail.put("error", "No ForwardProcess data => skip");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            info.setDetail(detail);
            return info;
        }

        // 4.1) 先保留前 forwardSteps 筆 (若總數 < forwardSteps，就全留)
        List<ForwardProcess> limitedList = allForward.subList(0, Math.min(forwardSteps, allForward.size()));

        // 4.2) 若 includeMeasurement=true => 只保留 eqpCategory="Measurement"
        List<ForwardProcess> filtered;
        if (includeMeasurement) {
            filtered = limitedList.stream()
                    .filter(fp -> "Measurement".equalsIgnoreCase(fp.getEqpCategory()))
                    .collect(Collectors.toList());
        } else {
            filtered = new ArrayList<>(limitedList);
        }

        // 5) 分開檢查 recipe 與 tool
        boolean passRecipe = checkRecipePatterns(filtered, recipeIds);
        boolean passTool = checkToolPatterns(filtered, toolIds);

        boolean pass = passRecipe && passTool;
        int lamp = pass ? 1 : 3;

        log.info("RuncardID: {} Condition: {} - passRecipe={}, passTool={}, finalLamp={}",
                runcardRawInfo.getRuncardId(), cond, passRecipe, passTool, lamp);

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("result", lamp);
        detailMap.put("forwardSteps", forwardSteps);
        detailMap.put("includedMeasurement", includeMeasurement);
        detailMap.put("configuredToolIdList", toolIds);
        detailMap.put("configuredRecipeIdList", recipeIds);

        List<String> forwardToolIds = filtered.stream()
                .map(ForwardProcess::getToolId)
                .collect(Collectors.toList());
        List<String> forwardRecipeIds = filtered.stream()
                .map(ForwardProcess::getRecipeId)
                .collect(Collectors.toList());
        detailMap.put("forwardToolIdList", forwardToolIds);
        detailMap.put("forwardRecipeIdList", forwardRecipeIds);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RuleForwardProcess check done, lamp={}",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }

    /**
     * 對於 recipeIds 裡的每個 pattern：
     * - 若 pattern 以 % 開頭 => 只要有 "任一" filteredForwardProcesses recipeId 包含該 partial 即可
     * - 若 pattern 不以 % 開頭 => 只要有 "任一" filteredForwardProcesses recipeId 完全等於該 pattern 即可
     * 若找不到 => fail
     * 需全部 pattern 都有找到對應 => pass
     */
    private boolean checkRecipePatterns(List<ForwardProcess> filteredForwardProcesses, List<String> configuredRecipeIds) {
        for (String pattern : configuredRecipeIds) {
            boolean matchedThisPattern = false;
            if (pattern.startsWith("%")) {
                String partial = pattern.substring(1);
                for (ForwardProcess fp : filteredForwardProcesses) {
                    if (fp.getRecipeId() != null && fp.getRecipeId().contains(partial)) {
                        matchedThisPattern = true;
                        break;
                    }
                }
            } else {
                for (ForwardProcess fp : filteredForwardProcesses) {
                    if (pattern.equals(fp.getRecipeId())) {
                        matchedThisPattern = true;
                        break;
                    }
                }
            }

            if (!matchedThisPattern) {
                return false;
            }
        }
        return true;
    }

    /**
     * toolIds 裡的每個 pattern => 需在 filteredForwardProcesses 中找到至少一筆 toolId==pattern
     * 若找不到 => fail
     */
    private boolean checkToolPatterns(List<ForwardProcess> filteredForwardProcesses, List<String> configuredToolIds) {
        for (String neededTool : configuredToolIds) {
            boolean matchedThis = false;
            for (ForwardProcess fp : filteredForwardProcesses) {
                if (neededTool.equals(fp.getToolId())) {
                    matchedThis = true;
                    break;
                }
            }
            if (!matchedThis) {
                return false;
            }
        }
        return true;
    }
}
