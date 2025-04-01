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
        // 準備：lotType 為空 => 期望跳過
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList()); // 空
        RuncardRawInfo rc = new RuncardRawInfo();

        // 執行
        ResultInfo result = ruleWaferCondition.check(rc, rule);

        // 驗證
        assertEquals(0, result.getResult(), "若 lotType 為空 => result=0");
        assertEquals("lotType is empty => skip check", result.getDetail().get("msg"));
        // 不會呼叫 dataLoaderService.getWaferCondition()
        verify(dataLoaderService, never()).getWaferCondition();
    }

    @Test
    void check_shouldCheckLotTypeTrue() {
        // 模擬: lotType 不空，但 RuleUtil.shouldCheckLotType(...) 回傳 true => skip
        // 這裡要 mock RuleUtil.shouldCheckLotType(...) => 因為是靜態方法 => 無法直接用 Mockito 去 stub
        // => 只能在 partId 設置讓 shouldCheckLotType(...) 返回 true
        // shouldCheckLotType(...) 內邏輯：只要 partId開頭=TM, lotType=C/W => false, or 反之 => true
        // 要做成 "Prod" + partId=XX => skip
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123"); // 不符合 Prod => shouldCheckLotType(...)=true => skip

        // 執行
        ResultInfo result = ruleWaferCondition.check(rc, rule);

        // 驗證
        assertEquals(0, result.getResult());
        assertEquals("lotType mismatch => skip check", result.getDetail().get("msg"));
        // 同理不會呼叫 getWaferCondition()
        verify(dataLoaderService, never()).getWaferCondition();
    }

    @Test
    void check_waferCondition_equal() {
        // lotType 不空, 且 shouldCheckLotType(...) 返回 false => 可以繼續檢查
        // Mock dataLoaderService.getWaferCondition() => uniqueCount=100, wfrQty=100 => 綠燈(1)
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod")); // partId=TM => pass
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC"); // => shouldCheckLotType(...)=false

        WaferCondition mockWf = new WaferCondition();
        mockWf.setUniqueCount("100");
        mockWf.setWfrQty("100");

        when(dataLoaderService.getWaferCondition()).thenReturn(mockWf);

        // 執行
        ResultInfo result = ruleWaferCondition.check(rc, rule);

        // 驗證
        assertEquals(1, result.getResult(), "uniqueCount == wfrQty => 綠燈(1)");
        assertEquals(true, result.getDetail().get("waferCondition"));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("experimentQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }

    @Test
    void check_waferCondition_notEqual() {
        // 模擬 uniqueCount != wfrQty => 紅燈(3)
        Rule rule = new Rule();
        rule.setLotType(Collections.singletonList("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");

        WaferCondition mockWf = new WaferCondition();
        mockWf.setUniqueCount("50");
        mockWf.setWfrQty("100");

        when(dataLoaderService.getWaferCondition()).thenReturn(mockWf);

        // 執行
        ResultInfo result = ruleWaferCondition.check(rc, rule);

        // 驗證
        assertEquals(3, result.getResult());
        assertEquals(false, result.getDetail().get("waferCondition"));
        assertEquals("50", String.valueOf(result.getDetail().get("experimentQty")));
        assertEquals("100", String.valueOf(result.getDetail().get("wfrQty")));

        verify(dataLoaderService, times(1)).getWaferCondition();
    }
}
