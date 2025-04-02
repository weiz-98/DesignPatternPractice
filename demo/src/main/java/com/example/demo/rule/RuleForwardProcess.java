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

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleForwardProcess implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(RuncardRawInfo runcardRawInfo, Rule rule) {
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        if (RuleUtil.isLotTypeEmpty(rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType is empty => skip check"));
            return info;
        }

        if (RuleUtil.isLotTypeMismatch(runcardRawInfo, rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType mismatch => skip check"));
            return info;
        }

        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "No settings => skip check"));
            return info;
        }

        int forwardSteps = RuleUtil.parseIntSafe(settings.get("forwardSteps"));
        boolean includeMeasurement = RuleUtil.parseBooleanSafe(settings.get("includeMeasurement"));
        List<String> recipeIds = RuleUtil.parseStringList(settings.get("recipeIds"));
        List<String> toolIds = RuleUtil.parseStringList(settings.get("toolIds"));

        List<ForwardProcess> allForward = dataLoaderService.getForwardProcess();
        if (allForward.isEmpty()) {
            info.setResult(3);
            info.setDetail(Collections.singletonMap("error", "No ForwardProcess data => skip"));
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

        // detail
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

        info.setResult(lamp);
        info.setDetail(detailMap);
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
