package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RuleInhibitionCheckStatusTest {

    @Mock
    private DataLoaderService dataLoaderService;

    @InjectMocks
    private RuleInhibitionCheckStatus ruleInhibitionCheckStatus;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * 情境 (A):
     * lotType 為空 => skip => result=0, "lotType is empty => skip check"
     * 不會呼叫 dataLoaderService.getInhibitionCheckStatus()
     */
    @Test
    void check_lotTypeEmpty() {
        Rule rule = new Rule();
        // lotType=null 或空 => skip
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("anything"); // 不重要, 反正lotTypeEmpty先攔

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    /**
     * 情境 (B):
     * shouldCheckLotType(...)=true => skip => "lotType mismatch => skip check"
     * => 只要 "Prod" + partId= "TM-something" => code => return true => skip
     */
    @Test
    void check_shouldCheckLotTypeTrue() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        // 讓 partId= "TM-123" => containProd && startsWithTM => true => skip
        rc.setPartId("TM-123");

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(0, info.getResult(), "若shouldCheckLotType=>true => skip=>0");
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    /**
     * 情境 (C): dataLoaderService回傳空 => skip => "No InhibitionCheckStatus => skip"
     * 要繼續到 dataLoaderService=> 先確保 shouldCheckLotType(...)=false => "不skip"
     * => e.g. lotType=["Prod"], partId="XX-999" => startsWithTM=false => containProd=true => => false => 不skip => next step
     */
    @Test
    void check_noInhibitionCheckData() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        // partId="XX-999" => startsWithTM=false & containProd=true => => shouldCheck= false => 不skip => 往下
        rc.setPartId("XX-999");

        // mock dataLoader => 回傳空
        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(Collections.emptyList());

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(3, info.getResult());
        assertEquals("No InhibitionCheckStatus data => skip", info.getDetail().get("error"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }

    /**
     * 情境 (D): allY => lamp=1(綠)
     * => 先不skip => lotType=["Prod"], partId="XX" => shouldCheck= false =>繼續
     * => dataLoaderService => all Y => 1
     */
    @Test
    void check_allY() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");  // startsWithTM=false => containProd=true => => false => 不skip

        List<InhibitionCheckStatus> mockList = List.of(
                new InhibitionCheckStatus("Y"),
                new InhibitionCheckStatus("Y")
        );
        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(mockList);

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(1, info.getResult(), "all Y => lamp=1");
        assertEquals(true, info.getDetail().get("inhibitionCheck"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }

    /**
     * 情境 (E): anyN => lamp=2(黃)
     */
    @Test
    void check_anyN() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        // same reason => partId="XX" => not skip => continue
        rc.setPartId("XX-ABC");

        List<InhibitionCheckStatus> mockList = List.of(
                new InhibitionCheckStatus("Y"),
                new InhibitionCheckStatus("N"),
                new InhibitionCheckStatus("Y")
        );
        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(mockList);

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(2, info.getResult(), "只要有N => lamp=2(黃)");
        assertEquals(false, info.getDetail().get("inhibitionCheck"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }
}
