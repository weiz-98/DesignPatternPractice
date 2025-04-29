package com.example.demo.utils;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class RuleUtil {

    // RuleUtil.java
    public static ResultInfo checkLotTypeEmpty(String cond,
                                               RuncardRawInfo rc,
                                               Rule rule) {

        if (isLotTypeEmpty(rule)) {
            ResultInfo info = new ResultInfo();
            info.setRuleType(rule.getRuleType());
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "lotType is empty => skip check");
            detail.put("runcardId", rc.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);

            log.info("RuncardID: {} Condition: {} - lotType is empty => skip check",
                    rc.getRuncardId(), cond);
            return info;
        }
        return null;
    }

    public static ResultInfo checkLotTypeMismatch(String cond,
                                                  RuncardRawInfo rc,
                                                  Rule rule) {

        if (isLotTypeMismatch(rc, rule)) {
            ResultInfo info = new ResultInfo();
            info.setRuleType(rule.getRuleType());
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "lotType mismatch => skip check");
            detail.put("runcardId", rc.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);

            log.info("RuncardID: {} Condition: {} - lotType mismatch => skip check",
                    rc.getRuncardId(), cond);
            return info;
        }
        return null;
    }

    public static ResultInfo checkSettingsNull(String cond,
                                               RuncardRawInfo rc,
                                               Rule rule) {

        if (rule.getSettings() == null) {
            ResultInfo info = new ResultInfo();
            info.setRuleType(rule.getRuleType());
            info.setResult(0);

            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "No settings => skip check");
            detail.put("runcardId", rc.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);

            log.info("RuncardID: {} Condition: {} - No settings => skip check",
                    rc.getRuncardId(), cond);
            return info;
        }
        return null;
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

    /**
     * 若輸入是 List<?>，且裡面每個元素都是 Map<String,String>，則將它們合併成一個大 Map，回傳
     * 若輸入直接就是 Map<String,String>，則直接轉型
     */
    @SuppressWarnings("unchecked")
    public static Map<String, String> parseStringMap(Object obj) {
        // 1) 若本來就是 Map => 直接轉
        if (obj instanceof Map) {
            try {
                return (Map<String, String>) obj;
            } catch (ClassCastException ex) {
                log.warn("[parseStringMap] cast fail. obj={}", obj, ex);
                return Collections.emptyMap();
            }
        }

        if (obj instanceof List<?> lst) {
            Map<String, String> combined = new LinkedHashMap<>();
            for (Object item : lst) {
                if (item instanceof Map) {
                    try {
                        Map<String, String> mapItem = (Map<String, String>) item;
                        combined.putAll(mapItem);
                    } catch (ClassCastException ex) {
                        log.warn("[parseStringMap] list item cast fail, item={}", item, ex);
                    }
                } else {
                    log.warn("[parseStringMap] list item is not a Map, item={}", item);
                }
            }
            return combined;
        }
        return Collections.emptyMap();
    }
}
