package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.example.demo.vo.RuncardResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Slf4j
class RunCardParserServiceTest {

    @InjectMocks
    private RunCardParserService runCardParserService;

    @Mock
    private IRuleValidator ruleValidator;

    private Runcard runcard;
    private List<Rule> rules;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // 模擬 Runcard 資料
        runcard = new Runcard("runcard1", "Atool", "2025-01-01", "approver1");

        // 模擬 Rules 資料
        rules = List.of(
                new Rule("ruleA", "group1"),
                new Rule("ruleB", "group1")
        );
    }

    @Test
    void testParse_withValidData() {
        // 模擬 ruleValidator 的行為
        when(ruleValidator.validateRule(runcard, rules)).thenReturn(List.of("pass", "fail"));
        when(ruleValidator.parseResult(List.of("pass", "fail"))).thenReturn("fail");

        // 測試 parse 方法
        RuncardResponse response = runCardParserService.parse(runcard, rules);
        log.info("testParse_withValidData response: {}", response);
        // 驗證回傳結果
        assertEquals("runcard1", response.getRuncardId());
        assertEquals(2, response.getResults().size());

        // 驗證結果的細節
        RuncardResponse.Result result1 = response.getResults().get(0);
        assertEquals("Atool", result1.getToolId());
        assertEquals("group1", result1.getToolGroupName());
        assertEquals("ruleA", result1.getRule());
        assertEquals("pass", result1.getResult());

        RuncardResponse.Result result2 = response.getResults().get(1);
        assertEquals("Atool", result2.getToolId());
        assertEquals("group1", result2.getToolGroupName());
        assertEquals("ruleB", result2.getRule());
        assertEquals("fail", result2.getResult());
    }

    @Test
    void testParse_withAllPass() {
        // 模擬所有 rule 都通過的情況
        when(ruleValidator.validateRule(runcard, rules)).thenReturn(List.of("pass", "pass"));
        when(ruleValidator.parseResult(List.of("pass", "pass"))).thenReturn("pass");

        // 測試 parse 方法
        RuncardResponse response = runCardParserService.parse(runcard, rules);
        log.info("testParse_withAllPass response: {}", response);
        // 驗證回傳結果
        assertEquals("runcard1", response.getRuncardId());
        assertEquals(2, response.getResults().size());
        assertEquals("pass", ruleValidator.parseResult(List.of("pass", "pass")));
    }

    @Test
    void testParse_withEmptyRules() {
        // 模擬沒有 rules 的情況
        List<Rule> emptyRules = List.of();
        when(ruleValidator.validateRule(runcard, emptyRules)).thenReturn(List.of());
        when(ruleValidator.parseResult(List.of())).thenReturn("not arrive");

        // 測試 parse 方法
        RuncardResponse response = runCardParserService.parse(runcard, emptyRules);
        log.info("testParse_withEmptyRules response: {}", response);
        // 驗證回傳結果
        assertEquals("runcard1", response.getRuncardId());
        assertEquals(0, response.getResults().size());
        assertEquals("not arrive", ruleValidator.parseResult(List.of()));
    }
}