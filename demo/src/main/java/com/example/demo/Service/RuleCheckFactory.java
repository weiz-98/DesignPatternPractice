package com.example.demo.Service;

import java.util.HashMap;
import java.util.Map;

public class RuleCheckFactory {

    private final Map<String, IRuleCheck> ruleCheckMap;

    public RuleCheckFactory() {
        this.ruleCheckMap = new HashMap<>();
        this.ruleCheckMap.put("ruleA", new RuleA());
        this.ruleCheckMap.put("ruleB", new RuleB());
    }

    public IRuleCheck getRuleCheck(String ruleName) {
        IRuleCheck ruleCheck = ruleCheckMap.get(ruleName);
        if (ruleCheck == null) {
            throw new IllegalArgumentException("No RuleCheck found for rule: " + ruleName);
        }
        return ruleCheck;
    }
}


