package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class DefaultRuleValidator implements IRuleValidator {

    private final RuleCheckFactory ruleCheckFactory;

    @Override
    public void preLayerProcess(Runcard runcard) {
        // 預處理，例如格式化或校驗 Runcard 的某些欄位
        System.out.println("Executing preLayerProcess for Runcard ID: " + runcard.getId());
    }

    @Override
    public List<String> validateRule(Runcard runcard, List<Rule> rules) {
        List<String> results = new ArrayList<>();
        // 根據每個規則名稱使用工廠模式取得對應的規則檢查實現
        for (Rule rule : rules) {
            IRuleCheck ruleCheck = ruleCheckFactory.getRuleCheck(rule.getName());
            results.add(ruleCheck.execute(runcard, rule));
        }
        return results;
    }

    @Override
    public String parseResult(List<String> results) {
        if (results.contains("fail")) {
            return "fail";
        }
        return results.stream().allMatch(result -> result.equals("pass")) ? "pass" : "not arrive";
    }
}

