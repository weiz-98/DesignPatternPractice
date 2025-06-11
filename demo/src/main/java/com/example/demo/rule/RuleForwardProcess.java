package com.example.demo.rule;

import com.example.demo.po.ForwardProcess;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.*;
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
    public ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule) {
        RuncardRawInfo runcardRawInfo = ruleExecutionContext.getRuncardRawInfo();
        String cond = ruleExecutionContext.getCond();
        RecipeToolPair recipeToolPair = ruleExecutionContext.getRecipeToolPair();
        log.info("RuncardID: {} Condition: {} - ForwardProcess check start", runcardRawInfo.getRuncardId(), cond);

        ResultInfo r;
        r = RuleUtil.skipIfLotTypeEmpty(cond, runcardRawInfo, rule, recipeToolPair);
        if (r != null) return r;
        r = RuleUtil.skipIfLotTypeMismatch(cond, runcardRawInfo, rule, recipeToolPair);
        if (r != null) return r;
        r = RuleUtil.skipIfSettingsNull(cond, runcardRawInfo, rule, recipeToolPair);
        if (r != null) return r;

        Map<String, Object> settings = rule.getSettings();

        int forwardSteps = RuleUtil.parseIntSafe(settings.get("forwardSteps"));
        boolean includeMeasurement = RuleUtil.parseBooleanSafe(settings.get("includeMeasurement"));
        List<String> recipeIds = RuleUtil.parseStringList(settings.get("recipeIds"));
        List<String> toolIds = RuleUtil.parseStringList(settings.get("toolIds"));

        log.info("RuncardID: {} Condition: {} - ForwardProcess configured => forwardSteps={}, includeMeasurement={}, recipeIds={}, toolIds={}", runcardRawInfo.getRuncardId(), cond, forwardSteps, includeMeasurement, recipeIds, toolIds);

        List<ForwardProcess> allForward = dataLoaderService.getForwardProcess(runcardRawInfo.getRuncardId());
        if (allForward.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No ForwardProcess data => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No ForwardProcess data => skip", false);
        }
        log.info("RuncardID: {} Condition: {} - ForwardProcess retrieved {} rows", runcardRawInfo.getRuncardId(), cond, allForward.size());

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

        log.info("RuncardID: {} Condition: {} - ForwardProcess check => passRecipe = '{}', passTool = '{}'", runcardRawInfo.getRuncardId(), cond, passRecipe, passTool);

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
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
        detailMap.put("lotType", rule.getLotType());

        log.info("RuncardID: {} Condition: {} - ForwardProcess detail = {}", runcardRawInfo.getRuncardId(), cond, detailMap);
        log.info("RuncardID: {} Condition: {} - ForwardProcess check done, lamp = '{}'", runcardRawInfo.getRuncardId(), cond, lamp);

        return ResultInfo.builder()
                .ruleType(rule.getRuleType())
                .result(lamp)
                .detail(detailMap)
                .build();
    }

    /**
     * 對於 recipeIds 裡的每個 pattern：
     * - %abc    -> 以 abc 結尾 (不分大小寫)
     * - abc%    -> 以 abc 開頭 (不分大小寫)
     * - %abc%   -> 內含 abc   (不分大小寫)
     * - 其餘    -> 完全相等   (不分大小寫)
     * 日後若要新增新型態 pattern，只要在 toMatcher(...) 加 case 即可，
     * 不必動主要迴圈邏輯，達到彈性擴充。
     */
    private boolean checkRecipePatterns(List<ForwardProcess> filteredForwardProcesses,
                                        List<String> configuredRecipeIds) {
        if (configuredRecipeIds == null || configuredRecipeIds.isEmpty()) {
            return true;
        }

        List<RecipePatternMatcher> matchers = configuredRecipeIds.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .map(this::toMatcher)
                .toList();

        // matcher 至少要命中一筆 ForwardProcess 才算 pass
        for (RecipePatternMatcher matcher : matchers) {
            boolean matched = filteredForwardProcesses.stream()
                    .map(fp -> fp.getRecipeId() == null ? "" : fp.getRecipeId())
                    .anyMatch(matcher::match);

            if (!matched) {
                return false; // 只要有一個 pattern 沒命中就 fail
            }
        }
        return true; // 全部 pattern 都命中
    }

    /**
     * 把字串 pattern 轉成對應的比對策略
     */
    private RecipePatternMatcher toMatcher(String rawPattern) {
        String p = rawPattern.trim();
        boolean startsWithPercent = p.startsWith("%");
        boolean endsWithPercent = p.endsWith("%");

        String core = p.replaceAll("^%|%$", ""); // 去掉左右各一個 %

        if (startsWithPercent && endsWithPercent) {
            // %abc%  → contains
            return recipeId -> recipeId.toLowerCase().contains(core.toLowerCase());
        }
        if (startsWithPercent) {
            // %abc   → endsWith
            return recipeId -> recipeId.toLowerCase().endsWith(core.toLowerCase());
        }
        if (endsWithPercent) {
            // abc%   → startsWith
            return recipeId -> recipeId.toLowerCase().startsWith(core.toLowerCase());
        }
        // 其餘 → 完全相等
        return recipeId -> recipeId.equalsIgnoreCase(p);
    }

    /**
     * 小函式介面：傳回 true 表示 recipeId 符合此 pattern
     */
    @FunctionalInterface
    private interface RecipePatternMatcher {
        boolean match(String recipeId);
    }


    /**
     * toolIds 裡的每個 pattern → 至少要在 filteredForwardProcesses 找到 1 筆符合
     * 規則與 recipe pattern 相同：支援 %, %xxx, xxx%, %xxx%
     */
    private boolean checkToolPatterns(List<ForwardProcess> filteredForwardProcesses,
                                      List<String> configuredToolIds) {
        if (configuredToolIds == null || configuredToolIds.isEmpty()) {
            return true;
        }

        List<RecipePatternMatcher> matchers = configuredToolIds.stream()
                .filter(p -> p != null && !p.trim().isEmpty())
                .map(this::toMatcher)
                .toList();

        // 每個 matcher 至少命中一筆 toolId
        for (RecipePatternMatcher matcher : matchers) {
            boolean matched = filteredForwardProcesses.stream()
                    .map(fp -> fp.getToolId() == null ? "" : fp.getToolId())
                    .anyMatch(matcher::match);

            if (!matched) {
                return false;   // 只要有一個 pattern 沒命中 → 整體 fail
            }
        }
        return true;            // 全部 pattern 都命中 → pass
    }

}
