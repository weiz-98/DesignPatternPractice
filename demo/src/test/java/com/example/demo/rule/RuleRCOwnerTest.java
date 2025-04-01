package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RuleRCOwnerTest {

    private final RuleRCOwner ruleRCOwner = new RuleRCOwner();

    @Test
    void check_lotTypeEmpty() {
        // lotType 為空 => skip
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());  // 空
        RuncardRawInfo rc = new RuncardRawInfo();

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(0, info.getResult(), "lotType empty => skip => result=0");
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_shouldCheckLotTypeTrue() {
        // 若 shouldCheckLotType(...) 回傳 true => skip
        // 內部邏輯: lotType包含"Prod" + partId不是TM => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC");  // 不符合 Prod => shouldCheckLotType= true => skip

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_noSettings() {
        // settings=null => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));  // 先讓lotType不空 & partId="TM" => 讓shouldCheckLotType=false
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");  // 符合 => shouldCheckLotType=>false => 會跑到settings檢查

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("No settings => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_foundEmployee() {
        // 準備: lotType=["Prod"], partId=TM => 讓它不skip
        // settings不為null，names 裡 => "employee name A" => "ENG-A"
        // issuingEngineer=ENG-A => found => 黃燈(2)
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", Map.of("Alice", "ENG-A", "Bob", "ENG-B"),
                "sections", Map.of("section1", "orgS1")  // 可有可無
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");           // => shouldCheckLotType=>false
        rc.setIssuingEngineer("ENG-A");   // => found => lamp=2

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(2, info.getResult(), "員工ID=ENG-A已在employeeIds => 黃燈");
        assertEquals("ENG-A", info.getDetail().get("issuingEngineer"));
        assertTrue(((List<String>) info.getDetail().get("configuredRCOwnerEmployeeId")).contains("ENG-A"));
        assertTrue(((List<String>) info.getDetail().get("configuredRCOwnerName")).contains("Alice"));
        assertEquals("section1", ((List<String>) info.getDetail().get("configuredRCOwnerOrg")).get(0));
    }

    @Test
    void check_notFoundEmployee() {
        // 模擬: issuingEngineer= "ENG-X" => 不在 namesMap 的 employeeIds => 綠燈(1)
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", Map.of("Alice", "ENG-A", "Bob", "ENG-B"),
                "sections", Map.of("section2", "orgS2")
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-XYZ");      // => shouldCheckLotType=>false
        rc.setIssuingEngineer("ENG-X"); // employeeIds=[ENG-A,ENG-B] => not found => lamp=1

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(1, info.getResult());
        assertEquals("ENG-X", info.getDetail().get("issuingEngineer"));
        // configuredRCOwnerEmployeeId = [ENG-A,ENG-B]
        List<String> empIds = (List<String>) info.getDetail().get("configuredRCOwnerEmployeeId");
        assertTrue(empIds.contains("ENG-A") && empIds.contains("ENG-B"));
        assertFalse(empIds.contains("ENG-X"));

        // sections => [section2]
        List<String> sec = (List<String>) info.getDetail().get("configuredRCOwnerOrg");
        assertEquals("section2", sec.get(0));
    }
}
