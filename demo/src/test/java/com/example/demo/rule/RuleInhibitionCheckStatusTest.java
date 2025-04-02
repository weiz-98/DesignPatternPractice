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
        Rule rule = new Rule();
        rule.setLotType(Collections.emptyList());
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("anything");

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("TM-123");

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(dataLoaderService, never()).getInhibitionCheckStatus();
    }

    @Test
    void check_noInhibitionCheckData() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-999");
        when(dataLoaderService.getInhibitionCheckStatus()).thenReturn(Collections.emptyList());

        ResultInfo info = ruleInhibitionCheckStatus.check(rc, rule);

        assertEquals(3, info.getResult());
        assertEquals("No InhibitionCheckStatus data => skip", info.getDetail().get("error"));
        verify(dataLoaderService, times(1)).getInhibitionCheckStatus();
    }

    @Test
    void check_allY() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-123");

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
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setPartId("XX-ABC");

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
