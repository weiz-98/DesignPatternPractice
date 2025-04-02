package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

import java.util.Collections;
@Component
public class RuleRecipeGroupCheckBlue implements IRuleCheck {

    @Override
    public ResultInfo check(String cond,RuncardRawInfo runcardRawInfo, Rule rule) {
        // 以下為範例邏輯，實際可用 rule.settings 去做判斷
        // 這裡假設簡單以 "ruleA" 就給予綠燈(1)
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());    // "ruleA"
        info.setResult(1);                       // 綠燈
        info.setDetail(Collections.singletonMap("msg", "RuleA pass"));
        return info;
    }
}
