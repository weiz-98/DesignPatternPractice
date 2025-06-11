package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuleExecutionContext;
import org.springframework.stereotype.Component;

@Component
public interface IRuleCheck {
    ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule);
}


