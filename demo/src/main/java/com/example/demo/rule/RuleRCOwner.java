package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

import java.util.Collections;
@Component
public class RuleRCOwner implements IRuleCheck {

    @Override
    public ResultInfo check(RuncardRawInfo runcardRawInfo, Rule rule) {
        // 模擬做一些條件判斷後 => 失敗(3)
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());    // "ruleB"
        info.setResult(3);                       // 紅燈
        info.setDetail(Collections.singletonMap("msg", "RuleB fail"));
        return info;
    }
}