package com.example.demo.rule;

import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.service.BatchCache;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.*;
import lombok.extern.slf4j.Slf4j;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class RuleRecipeGroupCheckBlueTest {

    @Mock
    private BatchCache cache;

    @InjectMocks
    private RuleRecipeGroupCheckBlue ruleRecipeGroupCheckBlue;

    private RuncardRawInfo dummyRuncard;
    private Rule ruleWithLotType;
    private static final String TEST_COND = "COND_TEST";

    private RuleExecutionContext ctx(String cond,
                                     RuncardRawInfo rc,
                                     String toolIds,
                                     String recipeId) {
        return RuleExecutionContext.builder()
                .cond(cond)
                .runcardRawInfo(rc)
                .recipeToolPair(
                        RecipeToolPair.builder()
                                .recipeId(recipeId)
                                .toolIds(toolIds)
                                .build())
                .build();
    }


    @BeforeEach
    void setUp() {
        dummyRuncard = new RuncardRawInfo();
        dummyRuncard.setRuncardId("RC-001");
        dummyRuncard.setPartId("TM-123");

        ruleWithLotType = new Rule();
        ruleWithLotType.setRuleType("RecipeGroupCheckBlue");
        ruleWithLotType.setLotType(List.of("Prod"));

        ruleWithLotType.setSettings(Map.of("someKey", "someVal"));
    }

    /**
     * case (1): recipeId = "...-{c}" => expansions = ["%%"]
     */
    @Test
    void testCase1_bracket_c() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-001",
                "JDTM16,JDTM17,JDTM20",
                "xxx.xx-xxxx.xxxx-{c}"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));

        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "ANY1", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "ANY2", "1", "1"),
                new RecipeGroupCheckBlue("JDTM20", "ANY3", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-001", List.of("JDTM16", "JDTM17", "JDTM20")))
                .thenReturn(checkBlueList);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17,JDTM20", "xxx.xx-xxxx.xxxx-{c}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        assertEquals(1, result.getDetail().get("result"));
        // failTools 應該空
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * case (2): recipeId = "...-{cEF}" => expansions = ["E","F"]
     */
    @Test
    void testCase2_bracket_cEF() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-002",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));


        // JDTM16 有 E, JDTM17 有 F
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "#E", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "#F", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-002", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);
        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{cEF}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * case (3): recipeId = "...-{cEF}{c134}" => expansions= AND => ["E","F"] AND ["1","3","4"]
     * 需對 tool 同時滿足( E或F ) 與 ( 1或3或4 ) 其中之一
     */
    @Test
    void testCase3_bracket_cEF_c134() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-003",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}{c134}"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));


        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                // JDTM16 => E,1
                new RecipeGroupCheckBlue("JDTM16", "#E", "1", "1"),
                new RecipeGroupCheckBlue("JDTM16", "#1", "1", "1"),

                // JDTM17 => F,3
                new RecipeGroupCheckBlue("JDTM17", "#F", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "#3", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-003", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{cEF}{c134}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * case (4): recipeId = "...-{c(3;2)}" => expansions = ["3","2"]
     * 也可以多個 {} -> e.g. {c(3;2)}{c(A;B)} => AND
     */
    @Test
    void testCase4_bracket_cParenMultiple() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-004",
                "JDTM16,JDTM20",
                "xxx.xx-xxxx.xxxx-{c(3;2)}"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));

        // - JDTM16, chamber=2 or 3 只要有一個 (release=1,enable=1) => pass
        // - JDTM20, chamber=2 or 3 只要有一個 => pass
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "#3", "1", "1"),
                new RecipeGroupCheckBlue("JDTM20", "#2", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-004", List.of("JDTM16", "JDTM20")))
                .thenReturn(checkBlueList);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM20", "xxx.xx-xxxx.xxxx-{c(3;2)}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * case (5): 沒有大括號 => parseChamberGrouped => expansions=[ [] ] (不限定 chamber)
     */
    @Test
    void testCase5_noBrackets() {
        // Mock => recipeId 無 { } => "xxx.xx-xxxx.xxxx-"
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-005",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));

        // 只要 tool=JDTM16,JDTM17 各有至少一筆 release=1, enable=1 => pass
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "XYZ", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "ABC", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-005", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * {cEF} 但 checkBlueList 沒有對應 E / F => fail => lamp=3
     */
    @Test
    void testCase2_failIfNoCorrespondingChamber() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-002",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}"
        );
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(List.of(info));

        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "#E", "1", "1")
                // JDTM17 => missing => fail
        );
        when(cache.getRecipeGroupCheckBlue("RG-002", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{cEF}"),
                ruleWithLotType);

        assertEquals(3, result.getResult());
        // failTools 應包含 "JDTM17"
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.contains("JDTM17"));
    }

    @Test
    void testLotTypeEmpty() {
        Rule ruleWithEmptyLotType = new Rule();
        ruleWithEmptyLotType.setRuleType("RecipeGroupCheckBlue");
        ruleWithEmptyLotType.setLotType(Collections.emptyList());
        ruleWithEmptyLotType.setSettings(Map.of("anyKey", "anyVal"));

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{cEF}"),
                ruleWithEmptyLotType);

        assertEquals(0, result.getResult());
        assertEquals("lotType is empty => skip check", result.getDetail().get("msg"));
    }

    @Test
    void testLotTypeMismatch() {
        // partId="XX-123" => mismatch
        dummyRuncard.setPartId("XX-123");

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx"),
                ruleWithLotType);

        assertEquals(0, result.getResult());
        assertEquals("lotType mismatch => skip check", result.getDetail().get("msg"));
    }

    @Test
    void testNoRecipeGroupsAndToolInfo() {
        // 表示找不到對應 cond
        when(cache.getRecipeGroupAndTool(anyString())).thenReturn(Collections.emptyList());

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{cEF}"),
                ruleWithLotType);

        assertEquals(3, result.getResult());
        assertEquals("No RecipeGroupsAndToolInfo for condition", result.getDetail().get("error"));
    }

    @Test
    void testEmptyCheckBlueList() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND, "RG-999", "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{c}"
        );
        when(cache.getRecipeGroupAndTool(anyString()))
                .thenReturn(List.of(info));

        when(cache.getRecipeGroupCheckBlue("RG-999",
                List.of("JDTM16", "JDTM17")))
                .thenReturn(Collections.emptyList());

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair(TEST_COND, "JDTM16,JDTM17", "xxx.xx-xxxx.xxxx-{c}"),
                ruleWithLotType);

        // failTools 包含 "JDTM16"
        assertEquals(3, result.getResult());
        @SuppressWarnings("unchecked")
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.contains("JDTM16"));
    }

    // ---------- _M pattern UT ----------

    @Test
    void cond_01_M01_useMultipleRecipeData() {
        // RecipeGroupAndTool：cond="01" (前半段)
        RecipeGroupAndTool rgt = new RecipeGroupAndTool(
                "01", "RG-A", "JDTM99", "recipe-{c}");
        when(cache.getRecipeGroupAndTool(anyString()))
                .thenReturn(List.of(rgt));

        // JDTM16、17 均 release=1, enable=1
        List<RecipeGroupCheckBlue> blues = List.of(
                new RecipeGroupCheckBlue("JDTM16", "ANY", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "ANY", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-A", List.of("JDTM16", "JDTM17")))
                .thenReturn(blues);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair("01_M01", "JDTM16,JDTM17", "recipe-{c}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        assertEquals("JDTM16,JDTM17", result.getDetail().get("toolIdList"));
    }

    @Test
    void cond_01_M02_fallbackToRecipeGroupToolIds() {
        RecipeGroupAndTool rgt = new RecipeGroupAndTool(
                "01", "RG-B", "JDTM20,JDTM21", "recipe-{c1}");
        when(cache.getRecipeGroupAndTool(anyString()))
                .thenReturn(List.of(rgt));

        // Blue 資料只需 cover 20、21
        List<RecipeGroupCheckBlue> blues = List.of(
                new RecipeGroupCheckBlue("JDTM20", "#1", "1", "1"),
                new RecipeGroupCheckBlue("JDTM21", "#1", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-B", List.of("JDTM20", "JDTM21")))
                .thenReturn(blues);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair("01_M02", "JDTM20,JDTM21", "recipe-{c1}"),
                ruleWithLotType);

        assertEquals(1, result.getResult());
        assertEquals("JDTM20,JDTM21", result.getDetail().get("toolIdList"));
    }

    /**
     * cond = "01_M03"，有對應 TOOL 清單但 Blue 資料不足 → 紅燈
     */
    @Test
    void cond_01_M03_failBecauseToolMismatch() {
        RecipeGroupAndTool rgt = new RecipeGroupAndTool(
                "01", "RG-C", "JDTM99", "recipe-{c}");
        when(cache.getRecipeGroupAndTool(anyString()))
                .thenReturn(List.of(rgt));

        // Blue 只給 JDTM30，缺 JDTM31
        List<RecipeGroupCheckBlue> blues = List.of(
                new RecipeGroupCheckBlue("JDTM30", "ANY", "1", "1")
        );
        when(cache.getRecipeGroupCheckBlue("RG-C", List.of("JDTM30", "JDTM31")))
                .thenReturn(blues);

        ResultInfo result = ruleRecipeGroupCheckBlue.check(
                stubPair("01_M03", "JDTM30,JDTM31", "recipe-{c}"),
                ruleWithLotType);

        assertEquals(3, result.getResult());
        @SuppressWarnings("unchecked")
        List<String> fail = (List<String>) result.getDetail().get("failTools");
        assertTrue(fail.contains("JDTM31"));
    }

    private RuleExecutionContext stubPair(String cond,
                                          String toolIds,
                                          String recipeId) {
        return ctx(cond, dummyRuncard, toolIds, recipeId);
    }
}
