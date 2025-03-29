package com.example.demo.Service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class RuleCheckFactory {

    // 可用來存放各種 ruleCheck 的實例
    private final Map<String, IRuleCheck> ruleCheckMap = new HashMap<>();

    // 建構子 or Init 進行註冊
    public RuleCheckFactory() {
        // 假設先放兩個 ruleCheck
        ruleCheckMap.put("ruleA", new RuleACheck());
        ruleCheckMap.put("ruleB", new RuleBCheck());
        // ...可繼續加...
    }

    public IRuleCheck getRuleCheck(String ruleType) {
        if (ruleCheckMap.containsKey(ruleType)) {
            return ruleCheckMap.get(ruleType);
        }
        // 沒找到就丟出例外或回傳預設
        throw new IllegalArgumentException("No RuleCheck found for ruleType: " + ruleType);
    }
}



