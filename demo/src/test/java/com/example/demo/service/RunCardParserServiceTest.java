package com.example.demo.service;

import com.example.demo.rule.DefaultRuleValidator;
import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RunCardParserServiceTest {

    @InjectMocks
    private RunCardParserService runCardParserService;

    @Mock
    private DefaultRuleValidator ruleValidator;

    @Mock
    private DataLoaderService dataLoaderService;

    // Dummy 資料
    private RuncardRawInfo dummyRawInfo;
    private OneConditionToolRuleMappingInfo conditionNoGroup;
    private RuncardMappingInfo mappingInfo;

    @BeforeEach
    void setUp() {
        // 建立 dummy RuncardRawInfo
        dummyRawInfo = new RuncardRawInfo();
        dummyRawInfo.setRuncardId("RC-001");
        Map<String, String> sectMap = Map.of(
                "JDTM10", "SectA",
                "JDTM11", "SectA",
                "JDTM20", "SectB"
        );
        lenient().when(dataLoaderService.getToolIdToSectNameMap())
                .thenReturn(sectMap);

        OneConditionRecipeAndToolInfo infoNoGroup = OneConditionRecipeAndToolInfo.builder()
                .condition("COND_NO_GROUP")
                .recipeId("REC-001")
                .toolIdList("JDTM10,JDTM11")
                .build();

        OneConditionRecipeAndToolInfo infoWithGroup = OneConditionRecipeAndToolInfo.builder()
                .condition("COND_WITH_GROUP")
                .recipeId("REC-002")
                .toolIdList("JDTM20")
                .build();

        lenient().when(dataLoaderService.getRecipeAndToolInfo("RC-001"))
                .thenReturn(List.of(infoNoGroup, infoWithGroup));

        // Condition 1: 無 group mapping
        conditionNoGroup = new OneConditionToolRuleMappingInfo();
        conditionNoGroup.setCondition("COND_NO_GROUP");
        conditionNoGroup.setToolChambers(Arrays.asList("JDTM10#A", "JDTM10#B"));
        conditionNoGroup.setGroupRulesMap(Collections.emptyMap());

        // Condition 2: 有 group mapping，condition 值為 "COND_WITH_GROUP"
        OneConditionToolRuleMappingInfo conditionWithGroup = new OneConditionToolRuleMappingInfo();
        conditionWithGroup.setCondition("COND_WITH_GROUP");
        conditionWithGroup.setToolChambers(List.of("JDTM20#C"));
        Rule ruleA = new Rule();
        ruleA.setRuleType("ruleA");
        Map<String, List<Rule>> groupMap = new HashMap<>();
        groupMap.put("GroupA", Collections.singletonList(ruleA));
        conditionWithGroup.setGroupRulesMap(groupMap);

        // 預設 mappingInfo 包含 Condition 1 與 Condition 2
        mappingInfo = new RuncardMappingInfo();
        mappingInfo.setRuncardRawInfo(dummyRawInfo);
        mappingInfo.setOneConditionToolRuleMappingInfos(Arrays.asList(conditionNoGroup, conditionWithGroup));
    }

    @Test
    void validateMappingRules_mixedConditions() {
        // 對於 conditionNoGroup，因 groupRulesMap 為空，
        // 根據程式邏輯應新增一筆 no-group 的 ResultInfo。
        // 對於 conditionWithGroup，模擬 ruleValidator.validateRule 回傳 dummy ResultInfo，
        // 並模擬 parseResult 直接回傳傳入的列表。

        // Stub：當 parseResult() 被呼叫時，直接回傳輸入列表
        when(ruleValidator.parseResult(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 對於 conditionWithGroup，stub validateRule()：假設返回一筆 result=1 (綠燈)
        ResultInfo dummyResult = new ResultInfo();
        dummyResult.setRuleType("ruleA");
        dummyResult.setResult(1);
        Map<String, Object> detail = new HashMap<>();
        detail.put("msg", "Pass from GroupA");
        dummyResult.setDetail(detail);

        when(ruleValidator.validateRule(eq("COND_WITH_GROUP"), eq(dummyRawInfo), anyList()))
                .thenReturn(Collections.singletonList(dummyResult));

        // 呼叫被測方法
        List<OneConditionToolRuleGroupResult> results = runCardParserService.validateMappingRules(mappingInfo);

        // 期望有 2 筆結果 (一個對應 conditionNoGroup、一個對應 conditionWithGroup)
        assertEquals(2, results.size());
        // Condition 1 => no-group
        Optional<OneConditionToolRuleGroupResult> noGroupOpt = results.stream()
                .filter(r -> "COND_NO_GROUP".equals(r.getCondition()))
                .findFirst();
        assertTrue(noGroupOpt.isPresent());
        OneConditionToolRuleGroupResult noGroupResult = noGroupOpt.get();
        assertEquals(Arrays.asList("JDTM10#A", "JDTM10#B"), noGroupResult.getToolChambers());
        assertNotNull(noGroupResult.getResults());
        assertEquals(1, noGroupResult.getResults().size());
        ResultInfo noGroupInfo = noGroupResult.getResults().get(0);
        assertEquals("no-group", noGroupInfo.getRuleType());
        assertEquals(0, noGroupInfo.getResult());
        assertEquals("No group matched for this condition", noGroupInfo.getDetail().get("msg"));

        // Condition 2 => ruleA=1
        Optional<OneConditionToolRuleGroupResult> withGroupOpt = results.stream()
                .filter(r -> "COND_WITH_GROUP".equals(r.getCondition()))
                .findFirst();
        assertTrue(withGroupOpt.isPresent());
        OneConditionToolRuleGroupResult withGroupResult = withGroupOpt.get();
        assertEquals(List.of("JDTM20#C"), withGroupResult.getToolChambers());
        assertNotNull(withGroupResult.getResults());
        assertEquals(1, withGroupResult.getResults().size());
        ResultInfo ruleAResult = withGroupResult.getResults().get(0);
        assertEquals("ruleA", ruleAResult.getRuleType());
        assertEquals(1, ruleAResult.getResult());
        // 驗證 group name 已經被加入到 detail
        assertEquals("GroupA", ruleAResult.getDetail().get("group"));
        assertEquals("Pass from GroupA", ruleAResult.getDetail().get("msg"));
    }

    @Test
    void validateMappingRules_BasicParams() {
        // 測試 mappingInfo 為 null
        assertTrue(runCardParserService.validateMappingRules(null).isEmpty());

        // 測試 mappingInfo 的 condition list 為空
        RuncardMappingInfo emptyMapping = new RuncardMappingInfo();
        emptyMapping.setRuncardRawInfo(dummyRawInfo);
        emptyMapping.setOneConditionToolRuleMappingInfos(Collections.emptyList());
        assertTrue(runCardParserService.validateMappingRules(emptyMapping).isEmpty());

        // 測試當 runcardRawInfo 為 null
        RuncardMappingInfo mappingWithNullRaw = new RuncardMappingInfo();
        mappingWithNullRaw.setRuncardRawInfo(null);
        mappingWithNullRaw.setOneConditionToolRuleMappingInfos(Collections.singletonList(conditionNoGroup));
        assertTrue(runCardParserService.validateMappingRules(mappingWithNullRaw).isEmpty());

        // 測試當 runcardRawInfo.runcardId 為 null
        RuncardRawInfo unknownRaw = new RuncardRawInfo();
        unknownRaw.setRuncardId(null);
        RuncardMappingInfo mappingWithUnknownId = new RuncardMappingInfo();
        mappingWithUnknownId.setRuncardRawInfo(unknownRaw);
        mappingWithUnknownId.setOneConditionToolRuleMappingInfos(Collections.singletonList(conditionNoGroup));
        assertTrue(runCardParserService.validateMappingRules(mappingWithUnknownId).isEmpty());
    }

    @Test
    void validateMappingRules_duplicateCondition() {
        // 模擬當多筆 OneConditionToolRuleMappingInfo 的 condition 值相同 (例如 "COND1")
        // 建立兩筆 mapping info，皆屬於 condition "COND1"
        OneConditionToolRuleMappingInfo mappingInfoDup1 = new OneConditionToolRuleMappingInfo();
        mappingInfoDup1.setCondition("COND1");
        mappingInfoDup1.setToolChambers(Collections.singletonList("Tool1#A"));
        // GroupA -> [ruleA]
        Rule ruleA = new Rule();
        ruleA.setRuleType("ruleA");
        Map<String, List<Rule>> groupMapDup1 = new HashMap<>();
        groupMapDup1.put("GroupA", Collections.singletonList(ruleA));
        mappingInfoDup1.setGroupRulesMap(groupMapDup1);

        OneConditionToolRuleMappingInfo mappingInfoDup2 = new OneConditionToolRuleMappingInfo();
        mappingInfoDup2.setCondition("COND1");
        mappingInfoDup2.setToolChambers(Collections.singletonList("Tool2#B"));
        // GroupB -> [ruleB]
        Rule ruleB = new Rule();
        ruleB.setRuleType("ruleB");
        Map<String, List<Rule>> groupMapDup2 = new HashMap<>();
        groupMapDup2.put("GroupB", Collections.singletonList(ruleB));
        mappingInfoDup2.setGroupRulesMap(groupMapDup2);

        // 建立新的 RuncardMappingInfo，包含兩筆 mapping info，且 condition 值均為 "COND1"
        RuncardMappingInfo duplicateMappingInfo = new RuncardMappingInfo();
        duplicateMappingInfo.setRuncardRawInfo(dummyRawInfo);
        duplicateMappingInfo.setOneConditionToolRuleMappingInfos(Arrays.asList(mappingInfoDup1, mappingInfoDup2));

        // Stub parseResult()：直接回傳輸入列表
        when(ruleValidator.parseResult(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Stub validateRule()：根據不同的 ruleType返回不同的 dummy ResultInfo
        ResultInfo dummyResultA = new ResultInfo();
        dummyResultA.setRuleType("ruleA");
        dummyResultA.setResult(1);
        Map<String, Object> detailA = new HashMap<>();
        detailA.put("msg", "Pass from GroupA");
        dummyResultA.setDetail(detailA);

        ResultInfo dummyResultB = new ResultInfo();
        dummyResultB.setRuleType("ruleB");
        dummyResultB.setResult(2);
        Map<String, Object> detailB = new HashMap<>();
        detailB.put("msg", "Pass from GroupB");
        dummyResultB.setDetail(detailB);

        // 當呼叫 validateRule("COND1", dummyRuncard, [ruleA]) => 回傳 dummyResultA
        when(ruleValidator.validateRule(eq("COND1"), eq(dummyRawInfo), argThat(rules ->
                rules != null && !rules.isEmpty() && "ruleA".equals(rules.get(0).getRuleType())
        ))).thenReturn(Collections.singletonList(dummyResultA));

        // 當呼叫 validateRule("COND1", dummyRawInfo, [ruleB]) => 回傳 dummyResultB
        when(ruleValidator.validateRule(eq("COND1"), eq(dummyRawInfo), argThat(rules ->
                rules != null && !rules.isEmpty() && "ruleB".equals(rules.get(0).getRuleType())
        ))).thenReturn(Collections.singletonList(dummyResultB));

        // 呼叫被測方法
        List<OneConditionToolRuleGroupResult> results = runCardParserService.validateMappingRules(duplicateMappingInfo);

        // 預期結果：由於有兩筆 mapping info，所以結果 List 大小為 2
        assertEquals(2, results.size());

        // 驗證第一筆 (mappingInfoDup1)
        OneConditionToolRuleGroupResult result1 = results.get(0);
        assertEquals("COND1", result1.getCondition());
        assertEquals(Collections.singletonList("Tool1#A"), result1.getToolChambers());
        assertNotNull(result1.getResults());
        assertEquals(1, result1.getResults().size());
        ResultInfo resInfo1 = result1.getResults().get(0);
        assertEquals("ruleA", resInfo1.getRuleType());
        assertEquals(1, resInfo1.getResult());
        assertEquals("GroupA", resInfo1.getDetail().get("group"));
        assertEquals("Pass from GroupA", resInfo1.getDetail().get("msg"));

        // 驗證第二筆 (mappingInfoDup2)
        OneConditionToolRuleGroupResult result2 = results.get(1);
        assertEquals("COND1", result2.getCondition());
        assertEquals(Collections.singletonList("Tool2#B"), result2.getToolChambers());
        assertNotNull(result2.getResults());
        assertEquals(1, result2.getResults().size());
        ResultInfo resInfo2 = result2.getResults().get(0);
        assertEquals("ruleB", resInfo2.getRuleType());
        assertEquals(2, resInfo2.getResult());
        assertEquals("GroupB", resInfo2.getDetail().get("group"));
        assertEquals("Pass from GroupB", resInfo2.getDetail().get("msg"));
    }
}
