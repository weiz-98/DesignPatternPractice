package com.example.demo.rule;

import com.example.demo.po.ForwardProcess;
import com.example.demo.service.BatchCache;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RuleForwardProcessTest {

    @Mock
    private BatchCache cache;

    @InjectMocks
    private RuleForwardProcess ruleForwardProcess;

    private RuleExecutionContext ctx(String cond, RuncardRawInfo rc) {
        RecipeToolPair emptyPair = RecipeToolPair.builder().recipeId("recipe01").toolIds("tool01").build();
        return RuleExecutionContext.builder()
                .cond(cond)
                .runcardRawInfo(rc)
                .recipeToolPair(emptyPair)
                .build();
    }

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        lenient().when(cache.getRecipeAndToolInfo(anyString()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));

        verify(cache, never()).getForwardProcess(anyString());
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(cache, never()).getForwardProcess(anyString());
    }

    @Test
    void check_noSettings() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setRuncardId("RC-001");

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(0, info.getResult());
        assertEquals("No settings => skip check", info.getDetail().get("msg"));
        verify(cache, never()).getForwardProcess(anyString());
    }

    @Test
    void check_noForwardProcess() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of("forwardSteps", 2));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setRuncardId("RC-001");

        when(cache.getForwardProcess(anyString())).thenReturn(Collections.emptyList());

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(3, info.getResult());
        assertEquals("No ForwardProcess data => skip", info.getDetail().get("error"));
        verify(cache, times(1)).getForwardProcess(anyString());
    }

    @Test
    void check_passAll() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 3,
                "includeMeasurement", false,
                "recipeIds", List.of("%A", "RCP1"),
                "toolIds", List.of("TOOL-X", "TOOL-999")
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setRuncardId("RC-001");

        List<ForwardProcess> all = new ArrayList<>();
        all.add(new ForwardProcess("LOT1", "preOpe1", "BABA", "TOOL-X", "2023-09-01T10:00", "someCat"));
        all.add(new ForwardProcess("LOT2", "preOpe2", "RCP1", "TOOL-999", "2023-09-01T11:00", "someCat"));
        all.add(new ForwardProcess("LOT3", "preOpe3", "Hello", "TOOL-ABC", "2023-09-01T12:00", "Measurement"));

        when(cache.getForwardProcess(anyString())).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult(), "all match");
        assertEquals(1, info.getDetail().get("result"));

        verify(cache, times(1)).getForwardProcess(anyString());
    }

    @Test
    void check_recipeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 2,
                "includeMeasurement", false,
                "recipeIds", List.of("%X", "ABC"),
                "toolIds", List.of("TOOL-999")
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-888");
        rc.setRuncardId("RC-001");

        List<ForwardProcess> all = List.of(
                new ForwardProcess("LOT1", "OPE1", "HelloX", "TOOL-999", "2023-09-01T10:00", "???"),
                new ForwardProcess("LOT2", "OPE2", "SOMEXXX", "TOOL-X", "2023-09-01T11:00", "???")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult(), "找不到 recipeId= 'ABC' => success =>1");
        verify(cache, times(1)).getForwardProcess(anyString());
    }

    @Test
    void check_toolMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 2,
                "includeMeasurement", false,
                "recipeIds", Collections.emptyList(),
                "toolIds", List.of("TOOL-1", "TOOL-2")
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123"); // => not skip
        rc.setRuncardId("RC-001");

        List<ForwardProcess> all = List.of(
                new ForwardProcess("LOT1", "OPE1", "RCPX", "TOOL-1", "2023-09-01T10:00", "???")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult(), "TOOL-2 不存在 => success =>1");
        verify(cache, times(1)).getForwardProcess(anyString());
    }

    @Test
    void check_includeMeasurement() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 5,
                "includeMeasurement", true,
                "recipeIds", List.of("%MeasureTest%"),
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-999");
        rc.setRuncardId("RC-001");

        List<ForwardProcess> all = new ArrayList<>();
        all.add(new ForwardProcess("LOT1", "pre1", "NoMeasureTest", "TOOL-ABC", "2023-09-01T09:00", "someCat"));
        all.add(new ForwardProcess("LOT2", "pre2", "HasMeasureTestInside", "TOOL-XYZ", "2023-09-01T10:00", "Measurement"));
        when(cache.getForwardProcess(anyString())).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult(), "only leave measurement");
        verify(cache, times(1)).getForwardProcess(anyString());
    }

    // ---------- recipe pattern unit-tests ----------

    /**
     * 1) 「完全相等」 (大小寫不敏感)
     */
    @Test
    void recipePattern_equalsIgnoreCase() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 3,
                "includeMeasurement", false,
                "recipeIds", List.of("AbC"),   // ← 不含 %，測 equalsIgnoreCase
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-001");
        rc.setRuncardId("RC-EQ");

        List<ForwardProcess> list = List.of(
                new ForwardProcess("L1", "O1", "abc", "TOOL-X", "2025-01-01T10:00", "cat")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(list);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult());
        verify(cache).getForwardProcess(anyString());
    }

    /**
     * 2) 「abc%」 → 以 abc 開頭 (大小寫不敏感)
     */
    @Test
    void recipePattern_startsWith() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 3,
                "includeMeasurement", false,
                "recipeIds", List.of("abc%"),  // ← startsWith
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-002");
        rc.setRuncardId("RC-ST");

        List<ForwardProcess> list = List.of(
                new ForwardProcess("L2", "O2", "AbcXYZ", "TOOL-X", "2025-01-01T11:00", "cat")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(list);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult());
        verify(cache).getForwardProcess(anyString());
    }

    /**
     * 3) 「%abc」 → 以 abc 結尾 (大小寫不敏感)
     */
    @Test
    void recipePattern_endsWith() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 3,
                "includeMeasurement", false,
                "recipeIds", List.of("%AbC"),  // ← endsWith
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-003");
        rc.setRuncardId("RC-END");

        List<ForwardProcess> list = List.of(
                new ForwardProcess("L3", "O3", "XYZaBc", "TOOL-X", "2025-01-01T12:00", "cat")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(list);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult());
        verify(cache).getForwardProcess(anyString());
    }

    /**
     * 4) 「%abc%」 → 內含 abc (大小寫不敏感)
     */
    @Test
    void recipePattern_contains() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 3,
                "includeMeasurement", false,
                "recipeIds", List.of("%aBc%"), // ← contains
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-004");
        rc.setRuncardId("RC-CTN");

        List<ForwardProcess> list = List.of(
                new ForwardProcess("L4", "O4", "xxxABCyyy", "TOOL-X", "2025-01-01T13:00", "cat")
        );
        when(cache.getForwardProcess(anyString())).thenReturn(list);

        ResultInfo info = ruleForwardProcess.check(ctx("TEST_COND", rc), rule);

        assertEquals(1, info.getResult());
        verify(cache).getForwardProcess(anyString());
    }

}
