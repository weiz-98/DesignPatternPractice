package com.example.demo.service;

import com.example.demo.rule.DefaultRuleValidator;
import com.example.demo.rule.IRuleCheck;
import com.example.demo.rule.RuleCheckFactory;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DefaultRuleValidatorTest {

    @Mock
    private RuleCheckFactory ruleCheckFactory;

    @InjectMocks
    private DefaultRuleValidator defaultRuleValidator;

    private RuncardRawInfo dummyRuncard;
    private Rule dummyRuleA;
    private Rule dummyRuleB;

    @BeforeEach
    void setUp() {
        dummyRuncard = new RuncardRawInfo();
        dummyRuncard.setRuncardId("RC-001");

        dummyRuleA = new Rule();
        dummyRuleA.setRuleType("ruleA");

        dummyRuleB = new Rule();
        dummyRuleB.setRuleType("ruleB");
    }

    @Test
    void validateRule_withValidRules() {
        // 模擬一個 IRuleCheck 會根據 ruleA 返回一個 ResultInfo
        IRuleCheck dummyChecker = (cond, runcard, rule) -> {
            ResultInfo info = new ResultInfo();
            info.setRuleType(rule.getRuleType());
            info.setResult(1); // 模擬綠燈
            Map<String, Object> detail = new HashMap<>();
            detail.put("msg", "Pass for ruleA");
            info.setDetail(detail);
            return info;
        };
        // 當查詢 ruleA 時，回傳 dummyChecker
        when(ruleCheckFactory.getRuleCheck("ruleA")).thenReturn(dummyChecker);

        // 建立一個規則清單，只包含 dummyRuleA
        List<Rule> rules = Collections.singletonList(dummyRuleA);

        List<ResultInfo> results = defaultRuleValidator.validateRule("TEST_COND", dummyRuncard, rules);
        log.info("results : {}", results);
        assertNotNull(results);
        assertEquals(1, results.size());
        ResultInfo result = results.get(0);
        assertEquals("ruleA", result.getRuleType());
        assertEquals(1, result.getResult());
        assertTrue(result.getDetail().containsKey("msg"));
        assertEquals("Pass for ruleA", result.getDetail().get("msg"));
    }

    @Test
    void validateRule_withExceptionInChecker() {
        // 模擬當查詢 ruleB 時，getRuleCheck 拋出例外
        when(ruleCheckFactory.getRuleCheck("ruleB")).thenThrow(new IllegalArgumentException("Checker not found"));

        List<Rule> rules = Collections.singletonList(dummyRuleB);
        List<ResultInfo> results = defaultRuleValidator.validateRule("TEST_COND", dummyRuncard, rules);

        assertNotNull(results);
        assertEquals(1, results.size());
        ResultInfo result = results.get(0);
        assertEquals("ruleB", result.getRuleType());
        // 預期 result 為紅燈
        assertEquals(3, result.getResult());
        // detail 應該包含 error 訊息
        assertTrue(result.getDetail().containsKey("error"));
        assertEquals("Checker not found", result.getDetail().get("error"));
    }

    @Test
    void validateRule_withEmptyRules() {
        // 當 rules 為 null 或空集合時，應返回空集合
        // 多加 "TEST_COND"
        List<ResultInfo> resultsNull = defaultRuleValidator.validateRule("TEST_COND", dummyRuncard, null);
        List<ResultInfo> resultsEmpty = defaultRuleValidator.validateRule("TEST_COND", dummyRuncard, Collections.emptyList());
        assertTrue(resultsNull.isEmpty());
        assertTrue(resultsEmpty.isEmpty());
    }

    @Test
    void parseResult_mergeSameRuleType() {
        // 建立多筆 ResultInfo 來自不同 group，且 ruleType 相同 (ruleA)
        ResultInfo info1 = new ResultInfo();
        info1.setRuleType("ruleA");
        info1.setResult(1);
        Map<String, Object> detail1 = new HashMap<>();
        detail1.put("group", "GroupA");
        detail1.put("msg", "Pass from GroupA");
        info1.setDetail(detail1);

        ResultInfo info2 = new ResultInfo();
        info2.setRuleType("ruleA");
        info2.setResult(3);
        Map<String, Object> detail2 = new HashMap<>();
        detail2.put("group", "GroupB");
        detail2.put("error", "Fail from GroupB");
        info2.setDetail(detail2);

        // 將兩筆結果放入 list
        List<ResultInfo> inputList = Arrays.asList(info1, info2);

        List<ResultInfo> consolidated = defaultRuleValidator.parseResult(inputList);
        log.info("consolidated : {}", consolidated);
        // 預期合併後只留一筆，因為 ruleType 相同
        assertEquals(1, consolidated.size());
        ResultInfo finalInfo = consolidated.get(0);
        assertEquals("ruleA", finalInfo.getRuleType());
        // 結果應為 3 (取最大燈號)
        assertEquals(3, finalInfo.getResult());
        // detail 合併後應包含 repeatedGroups, 並包含各 group 的資訊
        Map<String, Object> finalDetail = finalInfo.getDetail();
        assertNotNull(finalDetail);
        assertTrue(finalDetail.containsKey("repeatedGroups"));
        @SuppressWarnings("unchecked")
        List<String> groups = (List<String>) finalDetail.get("repeatedGroups");
        assertTrue(groups.contains("GroupA"));
        assertTrue(groups.contains("GroupB"));
        // 也會有合併的 key值，檢查其中一個
        assertEquals("Pass from GroupA", finalDetail.get("GroupA_msg"));
        assertEquals("Fail from GroupB", finalDetail.get("GroupB_error"));
    }

    @Test
    void parseResult_withAllNullResults() {
        // 測試若所有 result 都為 null，應預設為 3 並記 log.error (這裡只驗證返回結果)
        // 構造一筆 detail 為 null 或 result 為 null
        ResultInfo info = new ResultInfo();
        info.setRuleType("ruleX");
        info.setResult(null);
        info.setDetail(null);
        List<ResultInfo> inputList = Collections.singletonList(info);

        List<ResultInfo> consolidated = defaultRuleValidator.parseResult(inputList);
        assertEquals(1, consolidated.size());
        ResultInfo finalInfo = consolidated.get(0);
        assertEquals("ruleX", finalInfo.getRuleType());
        // 預設應為 3
        assertEquals(3, finalInfo.getResult());
    }
}
