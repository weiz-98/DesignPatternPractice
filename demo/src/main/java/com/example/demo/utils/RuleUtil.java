package com.example.demo.utils;

import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class RuleUtil {

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
            return false;
        }
        boolean startsWithTM = partId.startsWith("TM");

        boolean containsProd = lotTypeList.contains("Prod");
        boolean containsCW = lotTypeList.contains("C/W");

        boolean shouldCheck = containsProd && startsWithTM;

        if (containsCW && !startsWithTM) {
            shouldCheck = true;
        }

        return shouldCheck;
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
