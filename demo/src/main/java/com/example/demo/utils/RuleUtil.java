package com.example.demo.utils;

import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
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
    public static boolean isLotTypeInvalidity(RuncardRawInfo runcardRawInfo, Rule rule) {
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

    @SuppressWarnings("unchecked")
    public static Map<String, String> parseStringMap(Object obj) {
        if (obj instanceof Map) {
            try {
                return (Map<String, String>) obj;
            } catch (ClassCastException ex) {
                log.warn("parseStringMap cast fail, obj={}", obj);
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }
}
