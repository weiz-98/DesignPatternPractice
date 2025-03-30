package com.example.demo.Service;

import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunCardParserServiceTest {

    @InjectMocks
    private RunCardParserService runCardParserService;

    @Mock
    private DefaultRuleValidator ruleValidator;

    // Dummy 資料
    private RuncardRawInfo dummyRawInfo;
    private OneConditionToolRuleMappingInfo conditionNoGroup;
    private RuncardMappingInfo mappingInfo;

    @BeforeEach
    void setUp() {
        // 建立 dummy RuncardRawInfo
        dummyRawInfo = new RuncardRawInfo();
        dummyRawInfo.setRuncardId("RC-001");

        // Condition 1: 無 group mapping
        conditionNoGroup = new OneConditionToolRuleMappingInfo();
        conditionNoGroup.setCondition("COND_NO_GROUP");
        // 假設該 condition 下有兩個 toolChambers
        conditionNoGroup.setToolChambers(Arrays.asList("JDTM10#A", "JDTM10#B"));
        // groupRulesMap 為空
        conditionNoGroup.setGroupRulesMap(Collections.emptyMap());

        // Condition 2: 有 group mapping
        OneConditionToolRuleMappingInfo conditionWithGroup = new OneConditionToolRuleMappingInfo();
        conditionWithGroup.setCondition("COND_WITH_GROUP");
        conditionWithGroup.setToolChambers(Arrays.asList("JDTM20#C"));
        // 建立 groupRulesMap: 假設 GroupA 對應一筆 ruleA
        Rule ruleA = new Rule();
        ruleA.setRuleType("ruleA");
        Map<String, List<Rule>> groupMap = new HashMap<>();
        groupMap.put("GroupA", Collections.singletonList(ruleA));
        conditionWithGroup.setGroupRulesMap(groupMap);

        // 建立 RuncardMappingInfo，包含兩個 condition
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

        // 對於 conditionWithGroup，stub validateRule()：假設返回一筆 result，result=1 (綠燈)
        ResultInfo dummyResult = new ResultInfo();
        dummyResult.setRuleType("ruleA");
        dummyResult.setResult(1);
        Map<String, Object> detail = new HashMap<>();
        detail.put("msg", "Pass from GroupA");
        dummyResult.setDetail(detail);
        when(ruleValidator.validateRule(eq(dummyRawInfo), anyList()))
                .thenReturn(Collections.singletonList(dummyResult));

        // 呼叫被測方法
        List<OneConditionToolRuleGroupResult> results = runCardParserService.validateMappingRules(mappingInfo);

        // 期望有 2 筆結果 (一個對應 conditionNoGroup、一個對應 conditionWithGroup)
        assertEquals(2, results.size());

        // Condition 1 應為 no-group result
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

        // Condition 2 應返回 ruleA result
        Optional<OneConditionToolRuleGroupResult> withGroupOpt = results.stream()
                .filter(r -> "COND_WITH_GROUP".equals(r.getCondition()))
                .findFirst();
        assertTrue(withGroupOpt.isPresent());
        OneConditionToolRuleGroupResult withGroupResult = withGroupOpt.get();
        assertEquals(Arrays.asList("JDTM20#C"), withGroupResult.getToolChambers());
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
}
