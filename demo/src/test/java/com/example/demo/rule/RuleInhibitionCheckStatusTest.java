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

    @Test
    void check_lotTypeEmpty() {
        // given lotType 為空 => skip
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();

        // when
        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        // then
        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
        // 不會呼叫 dataLoaderService
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    @Test
    void check_shouldCheckLotTypeTrue() {
        // scenario: lotType=["Prod"], 但 partId="XX-123" => RuleUtil.shouldCheckLotType(...)=true => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123"); // partId 不以 TM 開頭 => mismatch => skip

        // when
        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        // then
        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    @Test
    void check_noInhibitionCheckData() {
        // lotType=["Prod"], partId="TM-ABC" => pass => 會呼叫 getInhibitionCheckStatus()
        // 但回傳空 => skip
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");

        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(Collections.emptyList());

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("No InhibitionCheckStatus => skip", info.getDetail().get("msg"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }

    @Test
    void check_allY() {
        // 所有 are "Y" => lamp=1 (綠)
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod")); // => 不skip
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");          // => shouldCheckLotType= false => 繼續

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

    @Test
    void check_anyN() {
        // 若任何一筆是 N => lamp=2 (黃)
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-ABC");

        List<InhibitionCheckStatus> mockList = List.of(
                new InhibitionCheckStatus("Y"),
                new InhibitionCheckStatus("N"),
                new InhibitionCheckStatus("Y")
        );
        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(mockList);

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(2, info.getResult());
        assertEquals(false, info.getDetail().get("inhibitionCheck"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }
}
