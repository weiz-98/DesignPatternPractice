package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.service.BatchCache;
import com.example.demo.service.DataLoaderService;
import com.example.demo.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class RuleInhibitionCheckStatusTest {

    @Mock
    private BatchCache cache;

    @InjectMocks
    private RuleInhibitionCheckStatus ruleInhibitionCheckStatus;

    private RuleExecutionContext ctx(String cond, RuncardRawInfo rc) {
        RecipeToolPair emptyPair = RecipeToolPair.builder().recipeId("recipe01").toolIds("tool01").build();
        return RuleExecutionContext.builder()
                .cond(cond)
                .runcardRawInfo(rc)
                .recipeToolPair(emptyPair)
                .cache(cache)
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
        rc.setPartId("anything");

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("TEST_COND", rc), rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType is empty => skip check", info.getDetail().get("msg"));

        verify(cache, never()).getInhibitionCheckStatus(anyString());
    }

    @Test
    void check_lotTypeMismatch() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("XX-123");

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("TEST_COND", rc), rule);

        assertEquals(0, info.getResult());
        assertEquals("lotType mismatch => skip check", info.getDetail().get("msg"));
        verify(cache, never()).getInhibitionCheckStatus(anyString());
    }

    @Test
    void check_noInhibitionCheckData() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-999");
        when(cache.getInhibitionCheckStatus(anyString())).thenReturn(Collections.emptyList());

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("TEST_COND", rc), rule);

        assertEquals(3, info.getResult());
        assertEquals("No InhibitionCheckStatus data => skip", info.getDetail().get("error"));
        verify(cache, times(1)).getInhibitionCheckStatus(anyString());
    }

    @Test
    void check_allY() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");

        List<InhibitionCheckStatus> mockList = List.of(
                new InhibitionCheckStatus("01", "Y"),
                new InhibitionCheckStatus("02", "N")
        );
        when(cache.getInhibitionCheckStatus(anyString())).thenReturn(mockList);

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("01", rc), rule);

        assertEquals(1, info.getResult(), "inhibitFlag=Y ⇒ lamp=1");
        assertEquals(true, info.getDetail().get("inhibitionCheck"));
        verify(cache, times(1)).getInhibitionCheckStatus(anyString());
    }

    @Test
    void check_anyN() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));

        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-ABC");

        List<InhibitionCheckStatus> mockList = List.of(
                new InhibitionCheckStatus("01", "Y"),
                new InhibitionCheckStatus("02", "N")
        );
        when(cache.getInhibitionCheckStatus(anyString())).thenReturn(mockList);

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("02", rc), rule);

        assertEquals(2, info.getResult(), "inhibitFlag≠Y ⇒ lamp=2");
        assertEquals(false, info.getDetail().get("inhibitionCheck"));
        verify(cache, times(1)).getInhibitionCheckStatus(anyString());
    }

    @Test
    void condWithM_shouldSkipMCondition() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");

        when(cache.getInhibitionCheckStatus(anyString()))
                .thenReturn(List.of(new InhibitionCheckStatus("01", "Y")));

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("01_M01", rc), rule);

        assertEquals(1, info.getResult());
        assertEquals(Boolean.TRUE, info.getDetail().get("inhibitionCheck"));

        verify(cache, times(1)).getInhibitionCheckStatus(anyString());
    }

    @Test
    void noRecordForCond_shouldSkip() {
        Rule rule = new Rule();
        rule.setLotType(List.of("Prod"));
        RuncardRawInfo rc = new RuncardRawInfo();
        rc.setRuncardId("RC-001");
        rc.setPartId("TM-123");

        List<InhibitionCheckStatus> list = List.of(
                new InhibitionCheckStatus("99", "Y")
        );
        when(cache.getInhibitionCheckStatus(anyString()))
                .thenReturn(list);

        ResultInfo info = ruleInhibitionCheckStatus.check(ctx("TEST_COND", rc), rule);

        assertEquals(3, info.getResult());
        assertEquals("No InhibitionCheckStatus for condition",
                info.getDetail().get("error"));

        verify(cache, times(1)).getInhibitionCheckStatus(anyString());
    }


}
