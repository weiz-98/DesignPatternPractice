package com.example.demo.rule;

import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.RecipeGroupAndTool;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class RuleRecipeGroupCheckBlueTest {

    @Mock
    private DataLoaderService dataLoaderService;

    @InjectMocks
    private RuleRecipeGroupCheckBlue ruleRecipeGroupCheckBlue;

    private RuncardRawInfo dummyRuncard;
    private Rule ruleWithLotType;
    private static final String TEST_COND = "COND_TEST";

    @BeforeEach
    void setUp() {
        // 建立 RuncardRawInfo (runcardId、partId等可自行設定)
        dummyRuncard = new RuncardRawInfo();
        dummyRuncard.setRuncardId("RC-001");
        dummyRuncard.setPartId("TM-123");

        // 建立一個預設帶有 lotType=["Prod"] 的 Rule
        ruleWithLotType = new Rule();
        ruleWithLotType.setRuleType("RecipeGroupCheckBlue");
        ruleWithLotType.setLotType(List.of("Prod"));
        // settings 先留空 map, 測試時可依需求再行調整
        ruleWithLotType.setSettings(Map.of("someKey", "someVal"));
    }

    /**
     * case (1): recipeId = "...-{c}" => expansions = ["%%"]
     */
    @Test
    void testCase1_bracket_c() {
        // 1) Mock dataLoaderService.getRecipeGroupAndToolInfo(anyString()):
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-001",
                "JDTM16,JDTM17,JDTM20",
                "xxx.xx-xxxx.xxxx-{c}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // 2) Mock dataLoaderService.getRecipeGroupCheckBlue(...)
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "ANY1", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "ANY2", "1", "1"),
                new RecipeGroupCheckBlue("JDTM20", "ANY3", "1", "1")
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-001", List.of("JDTM16", "JDTM17", "JDTM20")))
                .thenReturn(checkBlueList);

        // 3) 呼叫被測方法
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 4) 驗證結果 => 預期 pass => lamp=1
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
        // Mock dataLoaderService.getRecipeGroupAndToolInfo(anyString()) => recipeId帶 {cEF}
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-002",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // Mock checkBlueList => JDTM16 有 E, JDTM17 有 F
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "E", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "F", "1", "1")
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-002", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => pass => lamp=1
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
        // Mock dataLoaderService.getRecipeGroupAndToolInfo(anyString())
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-003",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}{c134}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // Mock checkBlueList
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                // JDTM16 => E,1
                new RecipeGroupCheckBlue("JDTM16", "E", "1", "1"),
                new RecipeGroupCheckBlue("JDTM16", "1", "1", "1"),

                // JDTM17 => F,3
                new RecipeGroupCheckBlue("JDTM17", "F", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "3", "1", "1")
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-003", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => 應該 pass => lamp=1
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
        // Mock dataLoaderService.getRecipeGroupAndToolInfo(anyString())
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-004",
                "JDTM16,JDTM20",
                "xxx.xx-xxxx.xxxx-{c(3;2)}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // Mock checkBlueList => e.g.:
        // - JDTM16, chamber=2 or 3 只要有一個 (release=1,enable=1) => pass
        // - JDTM20, chamber=2 or 3 只要有一個 => pass
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "3", "1", "1"),
                new RecipeGroupCheckBlue("JDTM20", "2", "1", "1")
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-004", List.of("JDTM16", "JDTM20")))
                .thenReturn(checkBlueList);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => pass => lamp=1
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
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // Mock => checkBlueList:
        // 只要 tool=JDTM16,JDTM17 各有至少一筆 release=1, enable=1 => pass
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "XYZ", "1", "1"),
                new RecipeGroupCheckBlue("JDTM17", "ABC", "1", "1")
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-005", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => pass => lamp=1
        assertEquals(1, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.isEmpty());
    }

    /**
     * 附加範例：假設 mismatch or missing data 造成 fail
     * 例如 case (2) {cEF} 但 checkBlueList 沒有對應 E / F => fail => lamp=3
     */
    @Test
    void testCase2_failIfNoCorrespondingChamber() {
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND,
                "RG-002",
                "JDTM16,JDTM17",
                "xxx.xx-xxxx.xxxx-{cEF}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // 這裡特意只給 JDTM16,E => 但沒有 JDTM17 E/F => JDTM17就 fail
        List<RecipeGroupCheckBlue> checkBlueList = List.of(
                new RecipeGroupCheckBlue("JDTM16", "E", "1", "1")
                // JDTM17 => missing => fail
        );
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-002", List.of("JDTM16", "JDTM17")))
                .thenReturn(checkBlueList);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => fail => lamp=3
        assertEquals(3, result.getResult());
        // failTools 應包含 "JDTM17"
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.contains("JDTM17"));
    }

    /**
     * 情境 A: lotType 為空 => skip => result=0
     */
    @Test
    void testLotTypeEmpty() {
        // 準備：把 lotType 設成空
        Rule ruleWithEmptyLotType = new Rule();
        ruleWithEmptyLotType.setRuleType("RecipeGroupCheckBlue");
        ruleWithEmptyLotType.setLotType(Collections.emptyList()); // 空
        ruleWithEmptyLotType.setSettings(Map.of("anyKey", "anyVal")); // 只要非null

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithEmptyLotType);

        // 驗證 => skip => result=0, msg="lotType is empty => skip check"
        assertEquals(0, result.getResult());
        assertEquals("lotType is empty => skip check", result.getDetail().get("msg"));
    }

    /**
     * 情境 B: lotType mismatch => skip => result=0
     *   - 若 partId 以 "TM" 開頭 => 只有 lotType=["Prod"] 時表示應檢查
     *   - 反之 => 會被視為 mismatch => skip
     */
    @Test
    void testLotTypeMismatch() {
        // 既然 ruleWithLotType=["Prod"]，那麼只在 partId 以"TM"開頭時才會檢查
        // 現在故意讓 partId="XX-123" => mismatch
        dummyRuncard.setPartId("XX-123");

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => skip => result=0, msg="lotType mismatch => skip check"
        assertEquals(0, result.getResult());
        assertEquals("lotType mismatch => skip check", result.getDetail().get("msg"));
    }

    /**
     * [新增] 情境 C: settings=null => skip => result=0
     */
    @Test
    void testNoSettings() {
        Rule ruleNoSettings = new Rule();
        ruleNoSettings.setRuleType("RecipeGroupCheckBlue");
        ruleNoSettings.setLotType(List.of("Prod"));
        ruleNoSettings.setSettings(null);

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleNoSettings);

        // 驗證 => skip => result=0, msg="No settings => skip check"
        assertEquals(0, result.getResult());
        assertEquals("No settings => skip check", result.getDetail().get("msg"));
    }

    /**
     * 情境 D: 找不到任何 RecipeGroupsAndToolInfo => result=3
     */
    @Test
    void testNoRecipeGroupsAndToolInfo() {
        // 模擬 dataLoaderService 直接回傳空 => 表示找不到對應 cond
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(Collections.emptyList());

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => 會走到 "No RecipeGroupsAndToolInfo for condition" => lamp=3
        assertEquals(3, result.getResult());
        assertEquals("No RecipeGroupsAndToolInfo for condition", result.getDetail().get("error"));
    }

    /**
     * 情境 E: checkBlueList 為空 => 無法匹配 => 失敗 => lamp=3
     *   (因為任何 tool/chamber 都找不到 release=1,enable=1 的紀錄)
     */
    @Test
    void testEmptyCheckBlueList() {
        // 先讓 cond 有一筆對應的 groupsAndToolInfo
        RecipeGroupAndTool info = new RecipeGroupAndTool(
                TEST_COND, "RG-999", "JDTM16", "xxx.xx-xxxx.xxxx-{c}"
        );
        when(dataLoaderService.getRecipeGroupAndToolInfo(anyString())).thenReturn(List.of(info));

        // 取得 checkBlueList 時, 回傳空 => 全部 tool 都 fail
        when(dataLoaderService.getRecipeGroupCheckBlue("RG-999", List.of("JDTM16")))
                .thenReturn(Collections.emptyList());

        // 執行
        ResultInfo result = ruleRecipeGroupCheckBlue.check(TEST_COND, dummyRuncard, ruleWithLotType);

        // 驗證 => fail => lamp=3, failTools 包含 "JDTM16"
        assertEquals(3, result.getResult());
        List<String> failTools = (List<String>) result.getDetail().get("failTools");
        assertTrue(failTools.contains("JDTM16"));
    }
}
