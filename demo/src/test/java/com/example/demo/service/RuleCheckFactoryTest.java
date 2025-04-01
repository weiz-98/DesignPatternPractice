package com.example.demo.service;

import com.example.demo.rule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.MockitoAnnotations.openMocks;

class RuleCheckFactoryTest {

    @Mock
    private RuleInhibitionCheckStatus ruleInhibitionCheckStatus;

    @Mock
    private RuleWaferCondition ruleWaferCondition;

    @Mock
    private RuleRCOwner ruleRCOwner;

    @Mock
    private RuleForwardProcess ruleForwardProcess;

    @Mock
    private RuleRecipeGroupCheckBlue ruleRecipeGroupCheckBlue;

    @InjectMocks
    private RuleCheckFactory factory;

    @BeforeEach
    void setUp() {
        // 1. 初始化 Mockito 注入
        openMocks(this);
        // 2. 呼叫 factory.init()，將注入的 Bean 放進 ruleCheckMap 裡
        factory.init();
    }

    @Test
    void getRuleCheck_validForwardProcess() {
        IRuleCheck ruleCheck = factory.getRuleCheck("ForwardProcess");
        assertNotNull(ruleCheck, "Expected non-null RuleCheck instance for 'ForwardProcess'");
        assertSame(ruleForwardProcess, ruleCheck, "Expected instance of ruleForwardProcess bean");
    }

    @Test
    void getRuleCheck_validInhibitionCheckStatus() {
        IRuleCheck ruleCheck = factory.getRuleCheck("InhibitionCheckStatus");
        assertNotNull(ruleCheck, "Expected non-null RuleCheck instance for 'InhibitionCheckStatus'");
        assertSame(ruleInhibitionCheckStatus, ruleCheck, "Expected instance of ruleInhibitionCheckStatus bean");
    }

    @Test
    void getRuleCheck_invalidRule() {
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getRuleCheck("invalidRule");
        });
        assertEquals("No RuleCheck found for ruleType: invalidRule", exception.getMessage());
    }
}
