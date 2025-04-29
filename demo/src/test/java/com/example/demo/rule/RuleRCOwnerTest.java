package com.example.demo.rule;

import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class RuleRCOwnerTest {

    private static final String COND = "TEST_COND";

    @Mock
    private DataLoaderService dataLoaderService;

    @InjectMocks
    private RuleRCOwner ruleRCOwner;

    /**
     * 每個 test 都用同一筆 OneConditionRecipeAndToolInfo
     */
    @BeforeEach
    void mockRecipeInfo() {
        OneConditionRecipeAndToolInfo info = OneConditionRecipeAndToolInfo.builder()
                .condition(COND)
                .recipeId("RECIPE-AAA")
                .toolIdList("")
                .build();
        lenient().when(dataLoaderService.getRecipeAndToolInfo(anyString()))
                .thenReturn(List.of(info));
    }


    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST_COND", rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST_COND", rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_noSettings() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST_COND", rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("No settings => skip check", info.getDetail().get("msg"));
    }

    @Test
    void check_foundEmployee() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", List.of(
                        Map.of("Alice", "ENG-A"),
                        Map.of("Bob", "ENG-B")
                ),
                "sections", List.of(
                        Map.of("sectionX", "orgX"),
                        Map.of("sectionY", "orgY")
                )
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("SEC1/ENG-A/Alice");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST_COND", rc, rule);

        assertEquals(2, info.getResult());
        assertEquals("Alice", info.getDetail().get("issuingEngineer"));

        List<String> empIds = (List<String>) info.getDetail().get("configuredRCOwnerEmployeeId");
        assertTrue(empIds.contains("ENG-A"));
        assertTrue(empIds.contains("ENG-B"));

        List<String> secs = (List<String>) info.getDetail().get("configuredRCOwnerOrg");
        assertTrue(secs.contains("sectionX"));
        assertTrue(secs.contains("sectionY"));
    }

    @Test
    void check_notFoundEmployee() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", List.of(
                        Map.of("Alice", "ENG-A"),
                        Map.of("Bob", "ENG-B")
                ),
                "sections", List.of(
                        Map.of("sectionX", "orgX"),
                        Map.of("sectionY", "orgY")
                )
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-999"); // => startWithTM=false => containProd => shouldCheck= false => pass
        rc.setIssuingEngineer("SEC2/ENG-X/Charlie");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST_COND", rc, rule);

        assertEquals(1, info.getResult());
        assertEquals("Charlie", info.getDetail().get("issuingEngineer"));

        List<String> empIds = (List<String>) info.getDetail().get("configuredRCOwnerEmployeeId");
        assertTrue(empIds.contains("ENG-A"));
        assertTrue(empIds.contains("ENG-B"));
        assertFalse(empIds.contains("ENG-X"));

        List<String> sec = (List<String>) info.getDetail().get("configuredRCOwnerOrg");
        assertEquals("sectionX", sec.get(0));
    }

    /**
     * issuingEngineer 僅單一字串，無 '/'，應直接比對
     */
    @Test
    void check_plainEngineerName() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", List.of(Map.of("Alice", "ENG-A")),
                "sections", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("Alice");          // ★ 無 '/'
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST", rc, rule);
        assertEquals(2, info.getResult());
    }

    /**
     * issuingEngineer 僅 "EmpId/EmpName" 兩段，也要能抓到最後一段
     */
    @Test
    void check_twoSegmentsEngineerName() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "names", List.of(Map.of("Bob", "ENG-B")),
                "sections", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("ENG-B/Bob");      // ★ 兩段
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleRCOwner.check("TEST", rc, rule);
        assertEquals(2, info.getResult());
    }
}
