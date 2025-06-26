package com.example.demo.service;

import com.example.demo.rule.DefaultRuleValidator;
import com.example.demo.rule.IRuleCheck;
import com.example.demo.rule.RuleCheckFactory;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@Slf4j
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DefaultRuleValidatorTest {

    private static final String COND = "TEST_COND";

    @Mock
    private RuleCheckFactory ruleCheckFactory;

    @Mock
    private BatchCache cache;

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

        OneConditionRecipeAndToolInfo pair = OneConditionRecipeAndToolInfo.builder().condition(COND).recipeId("RECIPE-X").toolIdList("TOOL1,TOOL2").build();
        lenient().when(cache.getRecipeAndToolInfo(anyString())).thenReturn(List.of(pair));

        lenient().when(cache.getToolIdToSectNameMap()).thenReturn(Map.of("TOOL1", "SectA", "TOOL2", "SectB"));
    }

    @Test
    void validateRule_withValidRules() {
        /* mock checker for ruleA → green lamp                          */
        IRuleCheck dummyChecker = (ctx, rule) -> {
            ResultInfo ri = new ResultInfo();
            ri.setRuleType(rule.getRuleType());
            ri.setResult(1);
            ri.setDetail(new HashMap<>(Map.of("msg", "Pass for ruleA")));
            return ri;
        };
        when(ruleCheckFactory.getRuleCheck("ruleA")).thenReturn(dummyChecker);

        List<ResultInfo> results = defaultRuleValidator.validateRule(COND, dummyRuncard, List.of(dummyRuleA),cache);

        assertEquals(1, results.size());
        ResultInfo res = results.get(0);

        assertEquals("ruleA", res.getRuleType());
        assertEquals(1, res.getResult());
        assertEquals("Pass for ruleA", res.getDetail().get("msg"));
        assertEquals("SectA,SectB", res.getDetail().get("conditionSectName"));
    }

    @Test
    void validateRule_withExceptionInChecker() {
        when(ruleCheckFactory.getRuleCheck("ruleB")).thenThrow(new IllegalArgumentException("Checker not found"));

        List<ResultInfo> results = defaultRuleValidator.validateRule(COND, dummyRuncard, List.of(dummyRuleB), cache);

        ResultInfo res = results.get(0);
        assertEquals(3, res.getResult());
        assertEquals("Checker not found", res.getDetail().get("error"));
        assertEquals("SectA,SectB", res.getDetail().get("conditionSectName"));
    }

    @Test
    void validateRule_withEmptyRules() {
        assertTrue(defaultRuleValidator.validateRule(COND, dummyRuncard, null,cache).isEmpty());
        assertTrue(defaultRuleValidator.validateRule(COND, dummyRuncard, Collections.emptyList(),cache).isEmpty());
    }

    @Test
    void parseResult_mergeSameRuleType() {
        ResultInfo info1 = new ResultInfo("ruleA", Map.of("group", "G1"), 1);
        ResultInfo info2 = new ResultInfo("ruleA", Map.of("group", "G2"), 3);

        List<ResultInfo> consolidated = defaultRuleValidator.parseResult(List.of(info1, info2));

        assertEquals(1, consolidated.size());
        ResultInfo fin = consolidated.get(0);
        assertEquals(3, fin.getResult());
        assertTrue(((List<?>) fin.getDetail().get("repeatedGroups")).containsAll(List.of("G1", "G2")));
    }

    // ─────────────────────────────────────────────────────────────────────────────
    @Test
    void parseResult_withAllNullResults() {
        ResultInfo info = new ResultInfo();
        info.setRuleType("ruleX");
        List<ResultInfo> consolidated = defaultRuleValidator.parseResult(List.of(info));

        assertEquals(3, consolidated.get(0).getResult());
    }
}
