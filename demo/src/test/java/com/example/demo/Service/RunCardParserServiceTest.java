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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunCardParserServiceTest {

    @InjectMocks
    private RunCardParserService runCardParserService;

    @Mock
    private DefaultRuleValidator ruleValidator;

    private RuncardRawInfo dummyRawInfo;
    private OneConditionToolRuleMappingInfo condNoGroup;
    private RuncardMappingInfo mappingInfo;

    @BeforeEach
    void setUp() {
        // 建立 dummy RuncardRawInfo
        dummyRawInfo = new RuncardRawInfo();
        dummyRawInfo.setRuncardId("RC-001");

        // 建立 condition 1：無 group mapping
        condNoGroup = new OneConditionToolRuleMappingInfo();
        condNoGroup.setCondition("COND_NO_GROUP");
        condNoGroup.setToolChambers(Arrays.asList("JDTM10#A", "JDTM11#B"));
        condNoGroup.setGroupRulesMap(Collections.emptyMap());

        // 建立 condition 2：有 group mapping
        OneConditionToolRuleMappingInfo condWithGroup = new OneConditionToolRuleMappingInfo();
        condWithGroup.setCondition("COND_WITH_GROUP");
        condWithGroup.setToolChambers(Arrays.asList("JDTM20#C"));
        // 建立 groupRulesMap：GroupA -> [ruleA]
        Rule ruleA = new Rule();
        ruleA.setRuleType("ruleA");
        Map<String, List<Rule>> groupMap = new HashMap<>();
        groupMap.put("GroupA", Collections.singletonList(ruleA));
        condWithGroup.setGroupRulesMap(groupMap);

        // 建立 RuncardMappingInfo，包含兩個 condition
        mappingInfo = new RuncardMappingInfo();
        mappingInfo.setRuncardRawInfo(dummyRawInfo);
        mappingInfo.setOneConditionToolRuleMappingInfos(Arrays.asList(condNoGroup, condWithGroup));
    }

    @Test
    void validateMappingRules_mixedConditions() {
        // 模擬：對於 condition 有 group mapping的部分，當呼叫 ruleValidator.validateRule 時，返回一筆 dummy ResultInfo
        ResultInfo partialResult = new ResultInfo();
        partialResult.setRuleType("ruleA");
        partialResult.setResult(1); // 假設綠燈
        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("msg", "Pass check");
        partialResult.setDetail(detailMap);
        // 當呼叫 validateRule() 並且 rules 非空時，回傳 partialResult（此處只模擬一筆）
        when(ruleValidator.validateRule(eq(dummyRawInfo), argThat(rules ->
                rules != null && !rules.isEmpty() && "ruleA".equals(rules.get(0).getRuleType())
        ))).thenReturn(Collections.singletonList(partialResult));
        // 模擬 parseResult()：直接回傳輸入列表（簡化）
        when(ruleValidator.parseResult(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // 呼叫被測方法
        List<OneConditionToolRuleGroupResult> results = runCardParserService.validateMappingRules(mappingInfo);
        // 驗證返回值不為 null，且有 2 筆 (一個對應於每個 condition)
        assertNotNull(results);
        assertEquals(2, results.size());

        // Condition 1（無 group mapping）的結果
        OneConditionToolRuleGroupResult resNoGroup = results.get(0);
        assertEquals("COND_NO_GROUP", resNoGroup.getCondition());
        assertEquals(Arrays.asList("JDTM10#A", "JDTM11#B"), resNoGroup.getToolChambers());
        List<ResultInfo> resNoGroupResults = resNoGroup.getResults();
        assertEquals(1, resNoGroupResults.size());
        ResultInfo noGroupInfo = resNoGroupResults.get(0);
        assertEquals("no-group", noGroupInfo.getRuleType());
        assertEquals(0, noGroupInfo.getResult());
        assertEquals("No group matched for this condition", noGroupInfo.getDetail().get("msg"));

        // Condition 2（有 group mapping）的結果
        OneConditionToolRuleGroupResult resWithGroup = results.get(1);
        assertEquals("COND_WITH_GROUP", resWithGroup.getCondition());
        assertEquals(Arrays.asList("JDTM20#C"), resWithGroup.getToolChambers());
        List<ResultInfo> resWithGroupResults = resWithGroup.getResults();
        // parseResult() 將 partialResults 傳回（此範例只有一筆）
        assertEquals(1, resWithGroupResults.size());
        ResultInfo ruleAResult = resWithGroupResults.get(0);
        assertEquals("ruleA", ruleAResult.getRuleType());
        assertEquals(1, ruleAResult.getResult());
        // 驗證 detail 中包含 group 字段，其值應為 "GroupA"
        assertEquals("GroupA", ruleAResult.getDetail().get("group"));
        // 同時 detail 中應包含原本的 "msg"
        assertEquals("Pass check", ruleAResult.getDetail().get("msg"));
    }

    @Test
    void validateMappingRules_BasicParamError() {
        // 測試當 mappingInfo 為 null 或沒有 condition 時，應回傳空集合
        List<OneConditionToolRuleGroupResult> result1 = runCardParserService.validateMappingRules(null);
        assertTrue(result1.isEmpty());

        RuncardMappingInfo emptyMapping = new RuncardMappingInfo();
        emptyMapping.setRuncardRawInfo(dummyRawInfo);
        emptyMapping.setOneConditionToolRuleMappingInfos(Collections.emptyList());
        List<OneConditionToolRuleGroupResult> result2 = runCardParserService.validateMappingRules(emptyMapping);
        assertTrue(result2.isEmpty());

        // 測試當 runcardRawInfo 為 null 時
        RuncardMappingInfo mappingWithNullRaw = new RuncardMappingInfo();
        mappingWithNullRaw.setRuncardRawInfo(null);
        mappingWithNullRaw.setOneConditionToolRuleMappingInfos(Arrays.asList(condNoGroup));
        List<OneConditionToolRuleGroupResult> result3 = runCardParserService.validateMappingRules(mappingWithNullRaw);
        assertTrue(result3.isEmpty());
    }
}
