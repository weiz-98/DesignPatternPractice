package com.example.demo.rule;

import com.example.demo.po.WaferCondition;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RuleWaferConditionTest {

    @Mock
    private DataLoaderService dataLoaderService;

    @InjectMocks
    private RuleWaferCondition ruleWaferCondition;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("whatever");
        rc.setRuncardId("RC-001");

        ResultInfo result = ruleWaferCondition.check("TEST_COND", rc, rule);

        assertEquals(0, result.getResult());
        assertEquals("lotType is empty => skip check", result.getDetail().get("msg"));
        verify(dataLoaderService, never()).getWaferCondition();
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");
        rc.setRuncardId("RC-001");

        ResultInfo result = ruleWaferCondition.check("TEST_COND", rc, rule);

        assertEquals(0, result.getResult());
        assertEquals("lotType mismatch => skip check", result.getDetail().get("msg"));
        verify(dataLoaderService, never()).getWaferCondition();
    }

    @Test
    void check_waferCondition_equal() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC");
        rc.setRuncardId("RC-001");

        WaferCondition wf = new WaferCondition();
        wf.setUniqueCount("100");
        wf.setWfrQty("100");
        when(dataLoaderService.getWaferCondition()).thenReturn(wf);

        ResultInfo result = ruleWaferCondition.check("TEST_COND", rc, rule);

        assertEquals(1, result.getResult(), "uniqueCount == wfrQty");
        assertEquals(true, result.getDetail().get("waferCondition"));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("experimentQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }

    @Test
    void check_waferCondition_notEqual() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999");
        rc.setRuncardId("RC-001");

        WaferCondition wf = new WaferCondition();
        wf.setUniqueCount("50");
        wf.setWfrQty("100");
        when(dataLoaderService.getWaferCondition()).thenReturn(wf);

        ResultInfo result = ruleWaferCondition.check("TEST_COND", rc, rule);

        assertEquals(3, result.getResult(), "uniqueCount != wfrQty");
        assertEquals(false, result.getDetail().get("waferCondition"));
        assertEquals("50", String.valueOf(result.getDetail().get("experimentQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }

    @Test
    void check_noWaferConditionData() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");
        rc.setRuncardId("RC-001");

        when(dataLoaderService.getWaferCondition()).thenReturn(null);

        ResultInfo result = ruleWaferCondition.check("TEST_COND", rc, rule);

        assertEquals(3, result.getResult(), "waferCondition=null");
        assertEquals("No WaferCondition data => skip", result.getDetail().get("error"));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }
}
