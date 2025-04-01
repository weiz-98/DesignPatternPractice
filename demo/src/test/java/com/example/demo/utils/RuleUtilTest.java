package com.example.demo.utils;

import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuleUtilTest {

    @Test
    void isLotTypeEmpty_nullLotType() {
        // 當 lotType = null
        Rule rule = new Rule();
        rule.setLotType(null);
        assertTrue(RuleUtil.isLotTypeEmpty(rule), "lotType 為 null 應視為 empty");
    }

    @Test
    void isLotTypeEmpty_emptyList() {
        // 當 lotType = []
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        assertTrue(RuleUtil.isLotTypeEmpty(rule), "lotType 為空集合應視為 empty");
    }

    @Test
    void isLotTypeEmpty_nonEmptyList() {
        // 當 lotType = ["Prod"]
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        assertFalse(RuleUtil.isLotTypeEmpty(rule), "lotType 不為空集合");
    }

    @Test
    void shouldCheckLotType_lotTypeEmpty() {
        // 當 lotType 為空 => 不檢查
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC"); // partId
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());

        assertFalse(RuleUtil.shouldCheckLotType(rc, rule),
                "lotType 為空時應該回傳 false => 不檢查");
    }

    @Test
    void shouldCheckLotType_partIdNull() {
        // partId = null => 不檢查
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId(null);
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod")); // 有值

        assertFalse(RuleUtil.shouldCheckLotType(rc, rule),
                "partId 為 null 時應該回傳 false => 不檢查");
    }

    @Test
    void shouldCheckLotType_containsProd_partIdStartsWithTM() {
        // lotType = ["Prod"], partId = "TMXXX"
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        assertTrue(RuleUtil.shouldCheckLotType(rc, rule),
                "包含 Prod + partId 前兩字 'TM' => 應該檢查 => true");
    }

    @Test
    void shouldCheckLotType_containsProd_partIdNotStartWithTM() {
        // lotType = ["Prod"], partId = "AB-123"
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("AB-123");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        assertFalse(RuleUtil.shouldCheckLotType(rc, rule),
                "只有 Prod 但 partId 不以 'TM' 開頭 => false");
    }

    @Test
    void shouldCheckLotType_containsCW_partIdStartWithTM() {
        // lotType = ["C/W"], partId = "TM-ABC"
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");
        Rule rule = new Rule();
        rule.setLotType(List.of("C/W"));

        assertFalse(RuleUtil.shouldCheckLotType(rc, rule),
                "只有 C/W 但 partId 開頭是 'TM' => false");
    }

    @Test
    void shouldCheckLotType_containsCW_partIdNotStartWithTM() {
        // lotType = ["C/W"], partId = "XX-456"
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-456");
        Rule rule = new Rule();
        rule.setLotType(List.of("C/W"));

        assertTrue(RuleUtil.shouldCheckLotType(rc, rule),
                "只有 C/W 且 partId 不以 'TM' 開頭 => true");
    }

    @Test
    void shouldCheckLotType_containsBoth() {
        // lotType = ["Prod", "C/W"], partId = "TM-ABC"
        // 只要符合其中一個 => true
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod", "C/W"));

        // partId 以 "TM" 開頭 => 符合 Prod => 應該 true
        assertTrue(RuleUtil.shouldCheckLotType(rc, rule),
                "同時包含 Prod & C/W，partId 開頭 'TM' => 符合 Prod => true");

        // 若換成 partId = "XX-999"，就只符合 C/W => 也應該 true
        rc.setPartId("XX-999");
        assertTrue(RuleUtil.shouldCheckLotType(rc, rule),
                "同時包含 Prod & C/W，partId 不以 'TM' => 符合 C/W => true");
    }
}
