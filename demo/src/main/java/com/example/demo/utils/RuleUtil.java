package com.example.demo.utils;

import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;

import java.util.List;

public class RuleUtil {

    private RuleUtil() {}

    /**
     * 判斷 lotType 是否為空 (null 或空清單).
     */
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
    public static boolean shouldCheckLotType(RuncardRawInfo runcardRawInfo, Rule rule) {
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
        boolean containsCW   = lotTypeList.contains("C/W");

        boolean shouldCheck = containsProd && startsWithTM;

        if (containsCW && !startsWithTM) {
            shouldCheck = true;
        }

        return shouldCheck;
    }
}
