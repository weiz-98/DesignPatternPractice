package com.example.demo.utils;

import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RuleUtilTest {

    @Test
    void isLotTypeEmpty_nullLotType() {
        // 當 lotType = null
        Rule rule = new Rule();
        rule.setLotType(null);
        assertTrue(RuleUtil.isLotTypeEmpty(rule));
    }

    @Test
    void isLotTypeEmpty_emptyList() {
        // 當 lotType = []
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        assertTrue(RuleUtil.isLotTypeEmpty(rule));
    }

    @Test
    void isLotTypeEmpty_nonEmptyList() {
        // 當 lotType = ["Prod"]
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        assertFalse(RuleUtil.isLotTypeEmpty(rule));
    }

    @Test
    void lotTypeEmpty_shouldReturnFalse() {
        // 當 lotType 為空 => 視為不檢查 => mismatch=false
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());

        assertFalse(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    @Test
    void partIdNull_shouldReturnTrueOrFalse() {

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId(null);
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        // 假設新的實作中：partId=null => return true(當 mismatch => skip)
        // 若您想改成 false => 看最終需求
        assertTrue(RuleUtil.isLotTypeMismatch(rc, rule),
                "若需求希望 partId=null => mismatch => true");
    }

    @Test
    void onlyProd_partIdStartsWithTM_shouldReturnFalse() {
        // 若只有 ["Prod"] => partId="TM" => 不 mismatch => return false
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        assertFalse(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    @Test
    void onlyProd_partIdNotStartWithTM_shouldReturnTrue() {
        // 若只有 ["Prod"] => partId不是 TM => mismatch => true
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        assertTrue(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    // ---------------------- 只有 ["C/W"] ----------------------

    @Test
    void onlyCW_partIdStartsWithTM_shouldReturnTrue() {
        // 若只有 ["C/W"] => partId=TM => mismatch
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");
        Rule rule = new Rule();
        rule.setLotType(List.of("C/W"));

        assertTrue(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    @Test
    void onlyCW_partIdNotStartWithTM_shouldReturnFalse() {
        // 若只有 ["C/W"] => partId非 TM => 不 mismatch => 應檢查
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-456");
        Rule rule = new Rule();
        rule.setLotType(List.of("C/W"));

        assertFalse(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    @Test
    void containsProdAndCW_partIdStartsWithTM_shouldReturnFalse() {
        // 若同時包含 => 全部都要檢查 => 不管 TM 或非 TM => mismatch=false
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod", "C/W"));

        assertFalse(RuleUtil.isLotTypeMismatch(rc, rule));
    }

    @Test
    void containsProdAndCW_partIdNotStartWithTM_shouldReturnFalse() {
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999");
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod", "C/W"));

        assertFalse(RuleUtil.isLotTypeMismatch(rc, rule));
    }

}
