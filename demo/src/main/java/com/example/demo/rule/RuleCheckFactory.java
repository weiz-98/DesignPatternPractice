package com.example.demo.rule;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@lombok.RequiredArgsConstructor
public class RuleCheckFactory {

    private final Map<String, IRuleCheck> ruleCheckMap = new HashMap<>();

    private final RuleInhibitionCheckStatus ruleInhibitionCheckStatus;
    private final RuleWaferCondition ruleWaferCondition;
    private final RuleRCOwner ruleRCOwner;
    private final RuleForwardProcess ruleForwardProcess;
    private final RuleRecipeGroupCheckBlue ruleRecipeGroupCheckBlue;

    public void init() {
        ruleCheckMap.put("InhibitionCheckStatus", ruleInhibitionCheckStatus);
        ruleCheckMap.put("WaferCondition", ruleWaferCondition);
        ruleCheckMap.put("RCOwner", ruleRCOwner);
        ruleCheckMap.put("ForwardProcess", ruleForwardProcess);
        ruleCheckMap.put("RecipeGroupCheckBlue", ruleRecipeGroupCheckBlue);
    }

    public IRuleCheck getRuleCheck(String ruleType) {
        if (ruleCheckMap.containsKey(ruleType)) {
            return ruleCheckMap.get(ruleType);
        }
        throw new IllegalArgumentException("No RuleCheck found for ruleType: " + ruleType);
    }
}
