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

    /**
     * (1) lotType 為空 => skip
     */
    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        // lotType 為空
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(0, info.getResult(), "lotType empty => skip => result=0");
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
    }

    /**
     * (2) shouldCheckLotType(...) => true => skip
     * 現有程式邏輯中:
     * boolean shouldCheck = containsProd && startsWithTM;
     * if (containsCW && !startsWithTM) shouldCheck=true;
     * 最後 if (shouldCheck) => skip => "lotType mismatch => skip check"
     * <p>
     * => 讓 partId=TM-XXX + lotType=["Prod"] 才會 shouldCheckLotType(...)=true
     */
    @Test
    void check_shouldCheckLotTypeTrue() {
        // 讓 partId="TM-ABC" + lotType=["Prod"]
        // => startsWithTM=true, containsProd=true => => shouldCheckLotType= true => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(0, info.getResult(), "應該skip=>0");
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
    }

    /**
     * (3) settings為null => skip
     * 先讓 partId="XX-123" + lotType=["C/W"] => 使 shouldCheckLotType(...)=false
     * => 如果 only "C/W" => partId不是TM => !startsWithTM => true => (該實作卻是true => skip)
     * => 反之, partId="TM" + Prod => also => true => skip
     * <p>
     * 所以這裡要選一個能讓 "shouldCheckLotType(...)=false" 的組合:
     * => e.g. lotType=["Prod"] + partId="XX" => (startsWithTM=false & containsProd=true => false => 不能 skip??)
     * 但實際您原始程式: "containsProd && startsWithTM => false", if(containsCW && !startsWithTM)=> ? ...
     * 這反轉邏輯較複雜, 這邊直接測試 partId=null => => shouldCheckLotType= false? => =>
     * <p>
     * 為避免邏輯衝突, 建議 partId="AB" & lotType=["C/W"] => startsWithTM=false => containsCW=true => => shouldCheckLotType= true => skip??
     * <p>
     * 若您現行程式中 want "不skip" => 需找"product" + "XX"? => again => false => skip
     * <p>
     * ---------------------------------------
     * "既然Naming矛盾" => 這裡只要確保 partId + lotType能讓shouldCheckLotType(...)= false
     * => e.g. lotType=["Prod","C/W"], partId="TT??"
     * => 由於 startsWithTM?? => false => but we have "C/W"? => code => if (containsCW && !startsWithTM) => shouldCheck= true
     * => again => skip?
     * <p>
     * => 這段Naming使測試難度加大. 下面給出一組,讓 "shouldCheckLotType= false" => partId= null => => => your code => returns false => *Wait, there's "partId==null => false"
     * => 這將繼續 => => 會到 settings check => skip => 0 => "No settings => skip check"
     * <p>
     * => Actually這就是我們想要 "settings=null => skip"
     */
    @Test
    void check_noSettings() {
        // 讓 partId=null => shouldCheckLotType(...) => false
        // => 讓程式繼續 => 之後遇到 settings==null => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));  // 先設 "Prod"
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId(null); // partId=null => "startsWithTM"=false => but code => if(partId==null) => return false => =>不skip => 進settings => skip

        ResultInfo info = ruleRCOwner.check(rc, rule);

        // 最後會被 "settings==null => skip" 截斷
        assertEquals(0, info.getResult());
        assertEquals("No settings => skip check", info.getDetail().get("msg"));
    }

    /**
     * (4) foundEmployee => lamp=2
     */
    @Test
    void check_foundEmployee() {
        // 只要 shouldCheckLotType(...)= false => partId=XX & lotType=Prod => => code => Actually is "startsWithTM=false, containProd=true => false => mismatch? => ???"
        // naming 使人困惑
        // => 這裡我們要 "partId=XX" + lotType= "C/W" => => startsWithTM=false & containCW=true => => shouldCheck= true => skip?
        // => 這會 skip => ???

        // => 反之 "partId=TM" + lotType= "Prod" => => shouldCheck= true => skip ???

        // => 假設: "partId=xxx" & lotType= "Prod","C/W" => => startsWithTM=false => containProd=true => false => but containCW & !startWithTM => => shouldCheck= true => skip ???

        // 由於 naming 矛盾, 我們臨時再"override" => partId= null => => => we get false => skip ??? It's contradictory.

        // "最簡單" => We forcibly choose partId= null => return false => then skip? => no. Actually that goes to next step => check => ???

        // 這裡先假設: "partId=anything" => code returns false => => 這裡 => let code returns false by "lotType=Empty"? => no => skip earlier
        // => or "lotType=???"

        // ================
        // *建議* 直接 "rename 'shouldCheckLotType => mismatchLotType' or fix code"
        // ================
        // 這裡假設 we want "found => 2"

        // let's just do "partId=AB" + lotType= [] => skip??? => conflict

        // ---------------------
        // *Workaround approach:*
        // 1) We want to run: code => skip the 1st if? => no => lotTypeEmpty? => false => good => means lotType!=empty
        // 2) skip the 2nd if? => means shouldCheckLotType(...)= false
        // => isLotTypeEmpty=> false => partId!=null => check => startsWithTM?? => if(Prod & TM => true => skip => not good
        // => if(CW & !TM => true => skip => not good
        // => => "Prod" & !TM => => false => we can continue => good => do next step => parse "names"? => found =>2
        // => so let's do "lotType=['Prod'] & partId='XX-123' => code => startsWithTM=false => containProd=true => => boolean shouldCheck= true&& false => false => final => false => => we can continue => good
        // => perfect => we do not skip => parse settings => found =>2

        Rule rule = new Rule();
        rule.setLotType(List.of("Prod")); // not empty
        // => partId= "XX-123" => startsWithTM=false => containProd=true => => shouldCheck= false => => pass
        rule.setSettings(Map.of(
                "names", Map.of("Alice", "ENG-A", "Bob", "ENG-B"),
                "sections", Map.of("section1", "orgS1")
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123"); // => startsWithTM=false => containsProd=true => => shouldCheck= false => => not skip
        rc.setIssuingEngineer("ENG-A"); // => found => lamp=2

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(2, info.getResult());
        assertEquals("ENG-A", info.getDetail().get("issuingEngineer"));
        List<String> empIds = (List<String>) info.getDetail().get("configuredRCOwnerEmployeeId");
        assertTrue(empIds.contains("ENG-A"));
        assertTrue(((List<String>) info.getDetail().get("configuredRCOwnerName")).contains("Alice"));
        List<String> secs = (List<String>) info.getDetail().get("configuredRCOwnerOrg");
        assertEquals("section1", secs.getFirst());
    }

    /**
     * (5) notFoundEmployee => lamp=1
     */
    @Test
    void check_notFoundEmployee() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        // same reason => partId="XX" => pass => not skip
        rule.setSettings(Map.of(
                "names", Map.of("Alice", "ENG-A", "Bob", "ENG-B"),
                "sections", Map.of("section2", "orgS2")
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999"); // => startWithTM=false => containProd => shouldCheck= false => pass
        rc.setIssuingEngineer("ENG-X"); // not in [ENG-A, ENG-B] => lamp=1

        ResultInfo info = ruleRCOwner.check(rc, rule);

        assertEquals(1, info.getResult());
        assertEquals("ENG-X", info.getDetail().get("issuingEngineer"));
        List<String> empIds = (List<String>) info.getDetail().get("configuredRCOwnerEmployeeId");
        assertTrue(empIds.contains("ENG-A"));
        assertTrue(empIds.contains("ENG-B"));
        assertFalse(empIds.contains("ENG-X"));
        List<String> sec = (List<String>) info.getDetail().get("configuredRCOwnerOrg");
        assertEquals("section2", sec.getFirst());
    }
}
