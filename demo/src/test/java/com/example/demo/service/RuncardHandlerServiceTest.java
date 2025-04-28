package com.example.demo.service;

import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class RuncardHandlerServiceTest {

    @InjectMocks
    private RuncardHandlerService runcardHandlerService;

    private RuncardRawInfo dummyRawInfo;
    private List<OneConditionRecipeAndToolInfo> recipeInfoList;
    private List<ToolRuleGroup> toolRuleGroups;

    @BeforeEach
    void setUp() {
        // 建立 dummy RuncardRawInfo
        dummyRawInfo = new RuncardRawInfo();
        dummyRawInfo.setRuncardId("RC-001");

        // 建立 dummy OneConditionRecipeAndToolInfo (假設 toolIdList 以逗號分隔，recipeId 內含 {cEF})
        OneConditionRecipeAndToolInfo recipeInfo1 = new OneConditionRecipeAndToolInfo();
        recipeInfo1.setCondition("COND1");
        recipeInfo1.setToolIdList("JDTM16,JDTM17");
        recipeInfo1.setRecipeId("xxx-{cEF}"); // 假設解析後會產生 ["JDTM16#E","JDTM17#E"]

        // 第二筆資料
        OneConditionRecipeAndToolInfo recipeInfo2 = new OneConditionRecipeAndToolInfo();
        recipeInfo2.setCondition("COND2");
        recipeInfo2.setToolIdList("JDTM20");
        recipeInfo2.setRecipeId("yyy-{cF}"); // 解析後 ["JDTM20#F"]

        recipeInfoList = Arrays.asList(recipeInfo1, recipeInfo2);

        // 建立 dummy ToolRuleGroup
        // GroupA，若有一筆 tool匹配 "JDTM16#E" 與 "JDTM20#F"
        ToolInfo toolA = new ToolInfo("DeptName1", "SecName2", "JDTM16", "E");
        ToolInfo toolB = new ToolInfo("DeptName1", "SecName2", "JDTM20", "F");
        ToolRuleGroup groupA = new ToolRuleGroup();
        groupA.setGroupName("GroupA");
        groupA.setTools(Arrays.asList(toolA, toolB));
        // 假設 groupA 的規則為 ruleA
        Rule ruleA = new Rule();
        ruleA.setRuleType("ruleA");
        groupA.setRules(Collections.singletonList(ruleA));

        // GroupB，假設匹配 "JDTM17#E"
        ToolInfo toolC = new ToolInfo("DeptName1", "SecName2", "JDTM17", "E");
        ToolRuleGroup groupB = new ToolRuleGroup();
        groupB.setGroupName("GroupB");
        groupB.setTools(Collections.singletonList(toolC));
        // GroupB 的規則為 ruleB
        Rule ruleB = new Rule();
        ruleB.setRuleType("ruleB");
        groupB.setRules(Collections.singletonList(ruleB));

        toolRuleGroups = Arrays.asList(groupA, groupB);
    }

    @Test
    void testBuildRuncardMappingInfo_Normal() {
        // 測試正常情況下，buildRuncardMappingInfo() 能正確產生 mappingInfo

        // 呼叫方法
        RuncardMappingInfo mappingInfo = runcardHandlerService.buildRuncardMappingInfo(dummyRawInfo, recipeInfoList, toolRuleGroups);

        // 檢查 mappingInfo 不為 null，且包含 2 筆 condition mapping
        assertNotNull(mappingInfo);
        assertEquals("RC-001", mappingInfo.getRuncardRawInfo().getRuncardId());
        assertNotNull(mappingInfo.getOneConditionToolRuleMappingInfos());
        assertEquals(2, mappingInfo.getOneConditionToolRuleMappingInfos().size());

        // 檢查第一筆 condition mapping資訊 (來自 recipeInfo1)
        OneConditionToolRuleMappingInfo condMapping1 = mappingInfo.getOneConditionToolRuleMappingInfos().getFirst();
        assertEquals("COND1", condMapping1.getCondition());
        // 利用 ParsingUtil 的預設實作，假設 "xxx-{cEF}" 可解析成 ["JDTM16#E", "JDTM17#E"]
        List<String> expectedChambers1 = Arrays.asList("JDTM16#E", "JDTM16#F", "JDTM17#E", "JDTM17#F");
        assertEquals(expectedChambers1, condMapping1.getToolChambers());
        // 檢查 groupRulesMap：對於 COND1，應該匹配到 GroupA 與 GroupB
        Map<String, List<Rule>> groupRulesMap1 = condMapping1.getGroupRulesMap();
        // 依照 dummy 資料：
        // recipeInfo1 toolChambers = ["JDTM16#E", "JDTM17#E"]
        // GroupA 有 tool "JDTM16#E" ，GroupB 有 tool "JDTM17#E"
        // 所以 groupRulesMap1 應該包含 GroupA -> [ruleA] 與 GroupB -> [ruleB]
        assertNotNull(groupRulesMap1);
        assertEquals(2, groupRulesMap1.size());
        assertTrue(groupRulesMap1.containsKey("GroupA"));
        assertTrue(groupRulesMap1.containsKey("GroupB"));

        // 檢查第二筆 condition mapping資訊 (來自 recipeInfo2)
        OneConditionToolRuleMappingInfo condMapping2 = mappingInfo.getOneConditionToolRuleMappingInfos().get(1);
        assertEquals("COND2", condMapping2.getCondition());
        List<String> expectedChambers2 = Collections.singletonList("JDTM20#F");
        assertEquals(expectedChambers2, condMapping2.getToolChambers());
        // 檢查 groupRulesMap2：recipeInfo2 toolChambers = ["JDTM20#F"]
        // GroupA 有 tool "JDTM20#F"，GroupB 沒匹配 => groupRulesMap2 只包含 GroupA
        Map<String, List<Rule>> groupRulesMap2 = condMapping2.getGroupRulesMap();
        assertNotNull(groupRulesMap2);
        assertEquals(1, groupRulesMap2.size());
        assertTrue(groupRulesMap2.containsKey("GroupA"));
    }

    @Test
    void testBuildRuncardMappingInfo_BasicParamsError() {
        // 測試基本參數錯誤時，應返回一個空 mappingInfo (或只帶原始資料)

        // 情境 1: runcardRawInfo 為 null
        RuncardMappingInfo mappingInfo1 = runcardHandlerService.buildRuncardMappingInfo(null, recipeInfoList, toolRuleGroups);
        assertNotNull(mappingInfo1);
        assertNull(mappingInfo1.getRuncardRawInfo());
        assertTrue(mappingInfo1.getOneConditionToolRuleMappingInfos().isEmpty());

        // 情境 2: recipeInfos 為空
        RuncardMappingInfo mappingInfo2 = runcardHandlerService.buildRuncardMappingInfo(dummyRawInfo, Collections.emptyList(), toolRuleGroups);
        assertNotNull(mappingInfo2);
        assertEquals("RC-001", mappingInfo2.getRuncardRawInfo().getRuncardId());
        assertTrue(mappingInfo2.getOneConditionToolRuleMappingInfos().isEmpty());

        // 情境 3: toolRuleGroups 為空
        RuncardMappingInfo mappingInfo3 = runcardHandlerService.buildRuncardMappingInfo(dummyRawInfo, recipeInfoList, Collections.emptyList());
        assertNotNull(mappingInfo3);
        assertEquals("RC-001", mappingInfo3.getRuncardRawInfo().getRuncardId());
        assertTrue(mappingInfo3.getOneConditionToolRuleMappingInfos().isEmpty());
    }

    @Test
    void testMappingRules() {
        // 測試 mappingRules() 方法，輸入一組 toolChambers 與 toolGroups，檢查返回結果
        // 假設輸入 toolChambers = ["JDTM16#E", "JDTM17#E", "JDTM20#F"]
        List<String> toolChambers = Arrays.asList("JDTM16#E", "JDTM17#E", "JDTM20#F");

        // 依據 setUp() 的 dummy 資料:
        // GroupA 擁有工具: "JDTM16#E", "JDTM20#F"
        // GroupB 擁有工具: "JDTM17#E"
        // 所以預期 mappingRules() 回傳 map 包含：
        //   "GroupA" -> [ruleA] 以及 "GroupB" -> [ruleB]
        Map<String, List<Rule>> resultMap = runcardHandlerService.mappingRules(toolChambers, toolRuleGroups);
        assertNotNull(resultMap);
        assertEquals(2, resultMap.size());
        assertTrue(resultMap.containsKey("GroupA"));
        assertTrue(resultMap.containsKey("GroupB"));
        // 驗證各 group 的 rule
        List<Rule> groupARules = resultMap.get("GroupA");
        assertNotNull(groupARules);
        assertEquals(1, groupARules.size());
        assertEquals("ruleA", groupARules.getFirst().getRuleType());

        List<Rule> groupBRules = resultMap.get("GroupB");
        assertNotNull(groupBRules);
        assertEquals(1, groupBRules.size());
        assertEquals("ruleB", groupBRules.getFirst().getRuleType());
    }

    // ---------- 新增覆蓋特殊比對邏輯 ----------

    /** Group 端 chamber="" ⇒ 只比對 toolId */
    @Test
    void groupChamberEmpty_onlyToolIdMatch() {
        // toolChambers 送入 "AAA#B"
        List<String> tcs = List.of("AAA#B");

        // toolGroup 內工具 AAA, chamber="" → 應視為匹配
        ToolInfo gTool = new ToolInfo("Dept", "Sec", "AAA", "");   // ★ chamber 空
        ToolRuleGroup grp = new ToolRuleGroup();
        grp.setGroupName("G_EMPTY");
        grp.setTools(List.of(gTool));
        Rule r = new Rule(); r.setRuleType("ruleEmpty");
        grp.setRules(List.of(r));

        Map<String, List<Rule>> res = runcardHandlerService.mappingRules(tcs, List.of(grp));
        assertEquals(1, res.size());
        assertTrue(res.containsKey("G_EMPTY"));
    }

    /** 條件端 #%% ⇒ wildcard，只要 toolId 一致即可 */
    @Test
    void tcWildcardPercentPercent_matchAnyChamber() {
        // toolChamber 帶 wildcard
        List<String> tcs = List.of("BBB#%%");

        // group 有 BBB#X
        ToolInfo gTool = new ToolInfo("D", "S", "BBB", "X");
        ToolRuleGroup grp = new ToolRuleGroup();
        grp.setGroupName("G_WC");
        grp.setTools(List.of(gTool));
        Rule r = new Rule(); r.setRuleType("ruleWC");
        grp.setRules(List.of(r));

        Map<String, List<Rule>> res = runcardHandlerService.mappingRules(tcs, List.of(grp));
        assertEquals(1, res.size());
        assertTrue(res.containsKey("G_WC"));
    }

    /** 兩邊皆指定 chamber 且不同 ⇒ 不應匹配 */
    @Test
    void requireBothToolAndChamberEqual() {
        // toolChamber: CCC#A
        List<String> tcs = List.of("CCC#A");

        // group 有 CCC#B → 不該匹配
        ToolInfo gTool = new ToolInfo("D", "S", "CCC", "B");
        ToolRuleGroup grp = new ToolRuleGroup();
        grp.setGroupName("G_STRICT");
        grp.setTools(List.of(gTool));
        Rule r = new Rule(); r.setRuleType("ruleStrict");
        grp.setRules(List.of(r));

        Map<String, List<Rule>> res = runcardHandlerService.mappingRules(tcs, List.of(grp));
        assertTrue(res.isEmpty(), "chamber 不同，應該不匹配");
    }

}
