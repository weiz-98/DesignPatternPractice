package com.example.demo.service;

import com.example.demo.rule.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
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
        Map<String, IRuleCheck> map = new HashMap<>();
        map.put("ForwardProcess", ruleForwardProcess);
        map.put("InhibitionCheckStatus", ruleInhibitionCheckStatus);
        map.put("WaferCondition", ruleWaferCondition);
        map.put("RCOwner", ruleRCOwner);
        map.put("RecipeGroupCheckBlue", ruleRecipeGroupCheckBlue);
        factory = new RuleCheckFactory(map);
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
        assertEquals("Unsupported ruleType: invalidRule", exception.getMessage());
    }
}
