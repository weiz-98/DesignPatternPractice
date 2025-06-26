package com.example.demo.rule;

import com.example.demo.po.IssuingEngineerInfo;
import com.example.demo.service.BatchCache;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RuleRCOwnerTest {

    private static final String COND = "TEST_COND";

    @Mock
    private BatchCache cache;

    @InjectMocks
    private RuleRCOwner ruleRCOwner;

    private RuleExecutionContext ctx(String cond, RuncardRawInfo rc) {
        RecipeToolPair emptyPair = RecipeToolPair.builder().recipeId("recipe01").toolIds("tool01").build();
        return RuleExecutionContext.builder()
                .cond(cond)
                .runcardRawInfo(rc)
                .recipeToolPair(emptyPair)
                .build();
    }

    @BeforeEach
    void stubRecipeInfo() {
        OneConditionRecipeAndToolInfo info = OneConditionRecipeAndToolInfo.builder()
                .condition(COND)
                .recipeId("RECIPE-AAA")
                .toolIdList("TOOL1,TOOL2")
                .build();
        lenient().when(cache.getRecipeAndToolInfo(anyString()))
                .thenReturn(List.of(info));
    }

    @Test
    void lotTypeEmpty_shouldSkip() {
        Rule rule = new Rule();
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(0, res.getResult());
        assertEquals("lotType is empty => skip check", res.getDetail().get("msg"));
    }

    @Test
    void lotTypeMismatch_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC");
        rc.setRuncardId("RC-001");

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(0, res.getResult());
        assertEquals("lotType mismatch => skip check", res.getDetail().get("msg"));
    }

    @Test
    void settingsNull_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo("RC-001", null, null,
                "TM-123", null, null, null, null, null);

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(0, res.getResult());
        assertEquals("No settings => skip check", res.getDetail().get("msg"));
    }


    @Test
    void foundByDivision_shouldReturnYellowLamp() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "divisions", List.of("DIV-1"),           // test division
                "departments", List.of("DEP-X"),
                "sections", List.of("SEC-X"),
                "employees", List.of("EMP-Z")
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("DEP-1/EMP-A/Alice");        // empId=EMP-A

        IssuingEngineerInfo eng = IssuingEngineerInfo.builder()
                .engineerId("EMP-A").engineerName("Alice")
                .divisionId("DIV-1").divisionName("DivName")
                .departmentId("DEP-1").departmentName("DepName")
                .sectionId("SEC-1").sectionName("SecName")
                .build();
        when(cache.getIssuingEngineerInfo(anyList()))
                .thenReturn(List.of(eng));

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(2, res.getResult());
        assertEquals("DEP-1/EMP-A/Alice", res.getDetail().get("issuingEngineer"));
    }

    @Test
    void noFieldMatches_shouldReturnGreenLamp() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "divisions", List.of("DIV-9"),    // no Match
                "departments", List.of("DEP-X"),    // no Match
                "sections", List.of("SEC-X"),    // no Match
                "employees", List.of("EMP-Z")     // no Match
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("DEP-1/EMP-A/Alice");

        IssuingEngineerInfo eng = IssuingEngineerInfo.builder()
                .engineerId("EMP-A").engineerName("Alice")
                .divisionId("DIV-1").departmentId("DEP-1").sectionId("SEC-1")
                .build();
        when(cache.getIssuingEngineerInfo(anyList()))
                .thenReturn(List.of(eng));

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(1, res.getResult());
    }


    @Test
    void issuingEngineerWithoutSlash_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "divisions", Collections.emptyList(),
                "departments", Collections.emptyList(),
                "sections", Collections.emptyList(),
                "employees", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("EMP-A/AliceId");              // only two '/'

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);

        assertEquals(3, res.getResult());
        assertEquals("issuingEngineer format unexpected (empId not found) => skip",
                res.getDetail().get("error"));
    }

    @Test
    void noIssuingEngineerInfo_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "divisions", Collections.emptyList(),
                "departments", Collections.emptyList(),
                "sections", Collections.emptyList(),
                "employees", Collections.emptyList()
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("DEP-A/EMP-A/Alice");      // empId = EMP-A

        when(cache.getIssuingEngineerInfo(anyList()))
                .thenReturn(Collections.emptyList());

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);
        assertEquals(3, res.getResult());
        assertEquals("No IssuingEngineerInfos data => skip",
                res.getDetail().get("error"));
    }

    @Test
    void noMatchedEngineer_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "divisions", Collections.emptyList(),
                "departments", Collections.emptyList(),
                "sections", Collections.emptyList(),
                "employees", Collections.emptyList()
        ));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");
        rc.setIssuingEngineer("DEP-A/EMP-A/Alice");      // empId = EMP-A

        // stub：only EMP-B → no match
        IssuingEngineerInfo other = IssuingEngineerInfo.builder()
                .engineerId("EMP-B").engineerName("Bob")
                .divisionId("DIV-X").departmentId("DEP-X").sectionId("SEC-X")
                .build();
        when(cache.getIssuingEngineerInfo(anyList()))
                .thenReturn(List.of(other));

        ResultInfo res = ruleRCOwner.check(ctx(COND, rc), rule);
        assertEquals(3, res.getResult());
        assertEquals("No matching IssuingEngineerInfo data => skip",
                res.getDetail().get("error"));
    }

}
