package com.example.demo.rule;

import com.example.demo.po.ForwardProcess;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
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
import static org.mockito.Mockito.*;

class RuleForwardProcessTest {

    @Mock
    private DataLoaderService dataLoaderService;

    @InjectMocks
    private RuleForwardProcess ruleForwardProcess;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));

        verify(dataLoaderService, never()).getForwardProcess();
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-999");

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getForwardProcess();
    }

    @Test
    void check_noSettings() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(null);
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("No settings => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getForwardProcess();
    }

    @Test
    void check_noForwardProcess() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of("forwardSteps", 2));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");

        when(dataLoaderService.getForwardProcess()).thenReturn(Collections.emptyList());

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(3, info.getResult());
        assertEquals("No ForwardProcess data => skip", info.getDetail().get("error"));
        verify(dataLoaderService, times(1)).getForwardProcess();
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
        rc.setPartId("XX-123");

        List<ForwardProcess> all = new ArrayList<>();
        all.add(new ForwardProcess("LOT1", "preOpe1", "BABA", "TOOL-X", "2023-09-01T10:00", "someCat"));
        all.add(new ForwardProcess("LOT2", "preOpe2", "RCP1", "TOOL-999", "2023-09-01T11:00", "someCat"));
        all.add(new ForwardProcess("LOT3", "preOpe3", "Hello", "TOOL-ABC", "2023-09-01T12:00", "Measurement"));

        when(dataLoaderService.getForwardProcess()).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(1, info.getResult(), "all match");
        assertEquals(1, info.getDetail().get("result"));

        verify(dataLoaderService, times(1)).getForwardProcess();
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
        rc.setPartId("XX-888");

        List<ForwardProcess> all = List.of(
                new ForwardProcess("LOT1", "OPE1", "HelloX", "TOOL-999", "2023-09-01T10:00", "???"),
                new ForwardProcess("LOT2", "OPE2", "SOMEXXX", "TOOL-X", "2023-09-01T11:00", "???")
        );
        when(dataLoaderService.getForwardProcess()).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(3, info.getResult(), "找不到 recipeId= 'ABC' => fail =>3");
        verify(dataLoaderService, times(1)).getForwardProcess();
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
        rc.setPartId("XX-123"); // => not skip

        List<ForwardProcess> all = List.of(
                new ForwardProcess("LOT1", "OPE1", "RCPX", "TOOL-1", "2023-09-01T10:00", "???")
        );
        when(dataLoaderService.getForwardProcess()).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(3, info.getResult(), "TOOL-2 不存在 => fail =>3");
        verify(dataLoaderService, times(1)).getForwardProcess();
    }

    @Test
    void check_includeMeasurement() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        rule.setSettings(Map.of(
                "forwardSteps", 5,
                "includeMeasurement", true,
                "recipeIds", List.of("%MeasureTest"),
                "toolIds", Collections.emptyList()
        ));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999");

        List<ForwardProcess> all = new ArrayList<>();
        all.add(new ForwardProcess("LOT1", "pre1", "NoMeasureTest", "TOOL-ABC", "2023-09-01T09:00", "someCat"));
        all.add(new ForwardProcess("LOT2", "pre2", "HasMeasureTestInside", "TOOL-XYZ", "2023-09-01T10:00", "Measurement"));
        when(dataLoaderService.getForwardProcess()).thenReturn(all);

        ResultInfo info = ruleForwardProcess.check(rc, rule);

        assertEquals(1, info.getResult(), "only leave measurement");
        verify(dataLoaderService, times(1)).getForwardProcess();
    }
}
