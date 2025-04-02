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

    /**
     * (1) lotType 為空 => skip
     */
    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        // lotType=null 或 empty => skip
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("whatever");

        ResultInfo result = ruleWaferCondition.check(rc, rule);

        assertEquals(0, result.getResult(), "若 lotType 為空 => result=0");
        assertEquals("lotType is empty => skip check", result.getDetail().get("msg"));
        verify(dataLoaderService, never()).getWaferCondition();
    }

    /**
     * (2) shouldCheckLotType(...) => true => skip => "lotType mismatch => skip check"
     * <p>
     * 根據現行實作:
     * boolean shouldCheck = (containsProd && startsWithTM)
     * if (containsCW && !startsWithTM) => shouldCheck=true
     * 最後 if(shouldCheck) => skip
     * <p>
     * 因此, 若 lotType=["Prod"], partId以"TM"開頭 => shouldCheckLotType(...)=true => skip
     */
    @Test
    void check_shouldCheckLotTypeTrue() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod")); // containsProd=true
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123"); // startsWithTM=true => shouldCheck=true => skip

        ResultInfo result = ruleWaferCondition.check(rc, rule);

        assertEquals(0, result.getResult());
        assertEquals("lotType mismatch => skip check", result.getDetail().get("msg"));
        verify(dataLoaderService, never()).getWaferCondition();
    }

    /**
     * (3) waferCondition_equal => 需先不skip => 代表 shouldCheckLotType(...)= false
     * => e.g. lotType=["Prod"], partId="XX-ABC" => startsWithTM=false & containProd=true => => false => 繼續
     * => dataLoaderService => uniqueCount=wfrQty => lamp=1(綠)
     */
    @Test
    void check_waferCondition_equal() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod")); // 只要 partId 不以"TM"開頭 => false
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC"); // startsWithTM=false => containProd=true => => false => 繼續

        // mock dataLoader => uniqueCount=100, wfrQty=100
        WaferCondition wf = new WaferCondition();
        wf.setUniqueCount("100");
        wf.setWfrQty("100");
        when(dataLoaderService.getWaferCondition()).thenReturn(wf);

        ResultInfo result = ruleWaferCondition.check(rc, rule);

        assertEquals(1, result.getResult(), "uniqueCount == wfrQty => 綠燈(1)");
        // waferCondition=true
        assertEquals(true, result.getDetail().get("waferCondition"));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("experimentQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }

    /**
     * (4) waferCondition_notEqual => lamp=3(紅)
     */
    @Test
    void check_waferCondition_notEqual() {
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999"); // => startsWithTM=false => containProd=true => => false => 繼續

        // mock dataLoader => uniqueCount != wfrQty => lamp=3
        WaferCondition wf = new WaferCondition();
        wf.setUniqueCount("50");
        wf.setWfrQty("100");
        when(dataLoaderService.getWaferCondition()).thenReturn(wf);

        ResultInfo result = ruleWaferCondition.check(rc, rule);

        assertEquals(3, result.getResult(), "uniqueCount != wfrQty => 紅燈(3)");
        assertEquals(false, result.getDetail().get("waferCondition"));
        assertEquals("50", String.valueOf(result.getDetail().get("experimentQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }

    @Test
    void check_noWaferConditionData() {
        Rule rule = new Rule();
        // 讓lotType不空 & shouldCheckLotType(...)= false => partId="XX"
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");

        // mock回傳 null
        when(dataLoaderService.getWaferCondition()).thenReturn(null);

        ResultInfo result = ruleWaferCondition.check(rc, rule);

        // 驗證 => 紅燈(3), detail => error=No WaferCondition data => skip
        assertEquals(3, result.getResult(), "若 waferCondition=null => 紅燈(3)");
        assertEquals("No WaferCondition data => skip", result.getDetail().get("error"));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }
}
