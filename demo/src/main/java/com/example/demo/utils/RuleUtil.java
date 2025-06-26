package com.example.demo.utils;

import com.example.demo.service.BatchCache;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.RecipeToolPair;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class RuleUtil {
    public static ResultInfo skipIfLotTypeEmpty(String cond,
                                                RuncardRawInfo rc,
                                                Rule rule,
                                                RecipeToolPair pair) {
        return isLotTypeEmpty(rule)
                ? buildSkipInfo(rule.getRuleType(), rc, cond, rule,
                pair, 0, "msg",
                "lotType is empty => skip check", false)
                : null;
    }

    public static ResultInfo skipIfLotTypeMismatch(String cond,
                                                   RuncardRawInfo rc,
                                                   Rule rule,
                                                   RecipeToolPair pair) {
        return isLotTypeMismatch(rc, rule)
                ? buildSkipInfo(rule.getRuleType(), rc, cond, rule,
                pair, 0, "msg",
                "lotType mismatch => skip check", false)
                : null;
    }

    public static ResultInfo skipIfSettingsNull(String cond,
                                                RuncardRawInfo rc,
                                                Rule rule,
                                                RecipeToolPair pair) {
        return rule.getSettings() == null
                ? buildSkipInfo(rule.getRuleType(), rc, cond, rule,
                pair, 0, "msg",
                "No settings => skip check", false)
                : null;
    }


    public static boolean isLotTypeEmpty(Rule rule) {
        List<String> lotTypeList = rule.getLotType();
        return lotTypeList == null || lotTypeList.isEmpty();
    }

    /**
     * 判斷這筆 RuncardRawInfo 是否需要做檢查 (依照 lotType 與 partId).
     * - 若 rule.lotType 為空 => 不檢查 (呼叫 isLotTypeEmpty)
     * - 若包含 "Prod" => partId 前兩字為 "TM" 才檢查
     * - 若包含 "C/W" => partId 前兩字不是 "TM" 才檢查
     * - 若同時包含 "Prod" 與 "C/W" => 只要符合其中一種即可檢查
     */
    public static boolean isLotTypeMismatch(RuncardRawInfo runcardRawInfo, Rule rule) {
        if (isLotTypeEmpty(rule)) {
            return false; // lotType 為空 => 不檢查
        }

        List<String> lotTypeList = rule.getLotType();
        String partId = runcardRawInfo.getPartId();
        if (partId == null) {
            // 這裡示範直接回傳 true（表示 mismatch）→ 跳過檢查
            return true;
        }

        boolean startsWithTM = partId.startsWith("TM");

        boolean containsProd = lotTypeList.contains("Prod");
        boolean containsCW = lotTypeList.contains("C/W");

        // 1) 若同時包含 "Prod" 與 "C/W" => 所有情況都檢查 ⇒ 不算 mismatch
        if (containsProd && containsCW) {
            return false; // no mismatch => 會繼續檢查
        }

        // 2) 若只有 "Prod" => 只檢查「partId 開頭 'TM'」，否則 mismatch
        if (containsProd && !containsCW) {
            // 若不是 TM 開頭 => mismatch
            return !startsWithTM;
        }

        // 3) 若只有 "C/W" => 只檢查「partId 不以 'TM' 開頭」，否則 mismatch
        if (containsCW && !containsProd) {
            // 若是 TM 開頭 => mismatch
            return startsWithTM;
        }

        // 4) 若 lotType 裡還有其他值 (或是完全不包含 Prod / C/W) => 不在需求範圍 → 全部視為 mismatch
        return true;
    }


    public static int parseIntSafe(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException ignore) {
            }
        }
        return 0;
    }

    public static boolean parseBooleanSafe(Object obj) {
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        if (obj instanceof String) {
            return Boolean.parseBoolean((String) obj);
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static List<String> parseStringList(Object obj) {
        if (obj instanceof List) {
            try {
                return (List<String>) obj;
            } catch (ClassCastException ex) {
                log.warn("parseStringList cast fail, obj={}", obj);
            }
        }
        return Collections.emptyList();
    }

    public static ResultInfo buildSkipInfo(String ruleType,
                                           RuncardRawInfo rc,
                                           String cond,
                                           Rule rule,
                                           RecipeToolPair pair,
                                           int result,            // 0=skip, 3=no data
                                           String key,            // "msg" or "error"
                                           String message,
                                           boolean isMCondition) {

        log.info("RuncardID: {} Condition: {} - '{}' - {}",
                rc.getRuncardId(), cond, key, message);

        Map<String, Object> d = new HashMap<>();
        d.put(key, message);
        d.put("recipeId", pair.getRecipeId());
        d.put("toolIds", pair.getToolIds());
        d.put("runcardId", rc.getRuncardId());
        d.put("condition", cond);
        d.put("lotType", rule.getLotType());
        if (isMCondition) {
            d.put("isMCondition", true);
        }

        return ResultInfo.builder()
                .ruleType(ruleType)
                .result(result)
                .detail(d)
                .build();
    }

    public static String buildConditionSectName(String toolIdsStr,
                                                BatchCache cache) {

        if (toolIdsStr == null || toolIdsStr.isBlank()) {
            return "";
        }

        Map<String, String> sectMap = cache.getToolIdToSectNameMap();
        if (sectMap == null || sectMap.isEmpty()) {
            return "";
        }

        return Arrays.stream(toolIdsStr.split(","))
                .map(String::trim)
                .filter(t -> !t.isEmpty())
                .map(sectMap::get)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.joining(","));
    }

    public static RecipeToolPair findRecipeToolPair(BatchCache cache,
                                                    String runcardId,
                                                    String condition) {

        return cache.getRecipeAndToolInfo(runcardId)
                .stream()
                .filter(o -> condition.equals(o.getCondition()))
                .findFirst()
                .map(o -> RecipeToolPair.builder()
                        .recipeId(o.getRecipeId())
                        .toolIds(o.getToolIdList())
                        .build())
                .orElseGet(() -> RecipeToolPair.builder()
                        .recipeId("")
                        .toolIds("")
                        .build());
    }
}
