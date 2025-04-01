package com.example.demo.rule;

import com.example.demo.po.ForwardProcess;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RuleForwardProcess implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(RuncardRawInfo runcardRawInfo, Rule rule) {
        // 建立回傳用
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        // 1) 檢查 lotType 為空 => skip
        if (RuleUtil.isLotTypeEmpty(rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType is empty => skip check"));
            return info;
        }

        // 2) 檢查是否要檢查 (partId & lotType)
        //   若 shouldCheckLotType(...) 回傳 true，表示不符合，需 skip
        if (RuleUtil.shouldCheckLotType(runcardRawInfo, rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType mismatch => skip check"));
            return info;
        }

        // 3) 檢查 settings
        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "No settings => skip check"));
            return info;
        }

        // 3.1) 解析設定
        int forwardSteps = parseIntSafe(settings.get("forwardSteps")); // 預設3
        boolean includeMeasurement = parseBooleanSafe(settings.get("includeMeasurement"));
        List<String> recipeIds = parseStringList(settings.get("recipeIds"));
        List<String> toolIds = parseStringList(settings.get("toolIds"));

        // 4) 從 dataLoaderService / ruleDao 取得 ForwardProcess
        List<ForwardProcess> allForward = dataLoaderService.getForwardProcess();
        if (allForward.isEmpty()) {
            // 沒有任何 ForwardProcess => 可能直接視為 skip / 或綠燈
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "No ForwardProcess => skip"));
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

        // 5) 檢查 recipeId, toolId
        //   只要有一筆不符合 => 紅燈(3)
        boolean pass = true;
        for (ForwardProcess fp : filtered) {
            // 檢查 recipeId
            if (!checkRecipeId(fp.getRecipeId(), recipeIds)) {
                pass = false;
                break;
            }
            // 檢查 toolId
            if (!checkToolId(fp.getToolId(), toolIds)) {
                pass = false;
                break;
            }
        }

        int lamp = pass ? 1 : 3; // 通過 => 綠(1)，否則紅(3)

        // 組裝 detail
        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("result", lamp);
        detailMap.put("forwardSteps", forwardSteps);
        detailMap.put("includedMeasurement", includeMeasurement);
        detailMap.put("configuredToolIdList", toolIds);
        detailMap.put("configuredRecipeIdList", recipeIds);
        // 取出留下的 ForwardProcess 的 recipeId / toolId
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
     * 若 settings.get("forwardSteps") 是整數 => 回傳該值，否則回傳預設 defaultVal
     */
    private int parseIntSafe(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException ignore) {
            }
        }
        return 3;
    }

    /**
     * 若 settings.get("includeMeasurement") 是 boolean => 直接回傳
     * 若是字串 "true"/"false" => 轉成 boolean
     * 否則回傳 defaultVal
     */
    private boolean parseBooleanSafe(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        }
        return false;
    }

    /**
     * 解析成 List<String>。若不是 List => 回傳空
     */
    @SuppressWarnings("unchecked")
    private List<String> parseStringList(Object obj) {
        if (obj instanceof List) {
            try {
                return (List<String>) obj;
            } catch (ClassCastException ex) {
                // 可視需求log.warn
            }
        }
        return Collections.emptyList();
    }

    /**
     * 檢查 forwardProcess.recipeId 是否符合 recipeIds 內每個 pattern。
     * - 若 pattern 以 '%' 開頭 => 表示只要 recipeId contains 去掉 '%' 的部份 即可通過
     * - 否則表示 recipeId 必須包含該 pattern 全字串
     * 若包含多個 pattern => 每個都要通過 => 回傳 true，否則 false
     */
    private boolean checkRecipeId(String actualRecipeId, List<String> recipeIds) {
        // 若 config 中沒指定任何 recipeIds => 視需求決定
        // 這裡假設「沒指定 => 不檢查 => pass」
        if (recipeIds.isEmpty()) return true;

        // 需要檢查
        for (String pattern : recipeIds) {
            if (pattern.startsWith("%")) {
                // 部分匹配 => actualRecipeId.contains(pattern.substring(1))
                String partial = pattern.substring(1);
                if (!actualRecipeId.contains(partial)) {
                    return false; // 不符 => fail
                }
            } else {
                // 全字串 => actualRecipeId 必須包含 pattern
                if (!actualRecipeId.contains(pattern)) {
                    return false;
                }
            }
        }
        // 都符合 => true
        return true;
    }

    /**
     * 檢查 forwardProcess.toolId 是否必須包含 toolIds 裡面所有元素(完整字串)。
     * "每個元素的 toolId 需有包含 toolIds 裡面所有元素的全名"
     * => actualToolId 必須對所有 configTool 都 .contains()
     */
    private boolean checkToolId(String actualToolId, List<String> configTools) {
        if (configTools.isEmpty()) {
            return true; // 沒指定 => pass
        }
        for (String t : configTools) {
            if (!actualToolId.contains(t)) {
                return false;
            }
        }
        return true;
    }
}
