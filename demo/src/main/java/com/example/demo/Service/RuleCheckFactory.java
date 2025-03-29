package com.example.demo.Service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RuleCheckFactory {

    private final Map<String, IRuleCheck> ruleCheckMap = new HashMap<>();

    public RuleCheckFactory() {
        // 假設先放兩個 ruleCheck
        ruleCheckMap.put("ruleA", new RuleACheck());
        ruleCheckMap.put("ruleB", new RuleBCheck());
    }

    public IRuleCheck getRuleCheck(String ruleType) {
        if (ruleCheckMap.containsKey(ruleType)) {
            return ruleCheckMap.get(ruleType);
        }
        throw new IllegalArgumentException("No RuleCheck found for ruleType: " + ruleType);
    }
}



