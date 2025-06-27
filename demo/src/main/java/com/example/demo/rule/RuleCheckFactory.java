package com.example.demo.rule;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class RuleCheckFactory {

    private final Map<String, IRuleCheck> checkerMap;

    public IRuleCheck getRuleCheck(String ruleType) {
        IRuleCheck checker = checkerMap.get(ruleType);
        if (checker == null) {
            throw new IllegalArgumentException("Unsupported ruleType: " + ruleType);
        }
        return checker;
    }
}
