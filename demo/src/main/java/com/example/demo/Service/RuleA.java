package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;

public class RuleA implements IRuleCheck {
    @Override
    public String execute(Runcard runcard, Rule rule) {
        // 模擬 RuleA 的判定邏輯
        return runcard.getToolId().startsWith("A") ? "pass" : "fail";
    }
}
