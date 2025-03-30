package com.example.demo.Service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuleCheckFactoryTest {

    @Test
    void getRuleCheck_validRuleA() {
        RuleCheckFactory factory = new RuleCheckFactory();
        IRuleCheck ruleCheck = factory.getRuleCheck("ruleA");
        assertNotNull(ruleCheck, "Expected non-null RuleCheck instance for 'ruleA'");
        assertInstanceOf(RuleACheck.class, ruleCheck, "Expected instance of RuleACheck for 'ruleA'");
    }

    @Test
    void getRuleCheck_validRuleB() {
        RuleCheckFactory factory = new RuleCheckFactory();
        IRuleCheck ruleCheck = factory.getRuleCheck("ruleB");
        assertNotNull(ruleCheck, "Expected non-null RuleCheck instance for 'ruleB'");
        assertInstanceOf(RuleBCheck.class, ruleCheck, "Expected instance of RuleBCheck for 'ruleB'");
    }

    @Test
    void getRuleCheck_invalidRule() {
        RuleCheckFactory factory = new RuleCheckFactory();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.getRuleCheck("invalidRule");
        });
        assertEquals("No RuleCheck found for ruleType: invalidRule", exception.getMessage());
    }
}
