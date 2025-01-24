package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.example.demo.vo.RuncardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class RunCardParserService {

    private final IRuleValidator ruleValidator;

    // 初始化時注入工廠和規則校驗器
    public RunCardParserService() {
        this(new DefaultRuleValidator(new RuleCheckFactory()));
    }

    public RuncardResponse parse(Runcard runcard, List<Rule> rules) {
        // 執行前置處理
        ruleValidator.preLayerProcess(runcard);

        // 判定每個 Rule 的結果
        List<String> ruleResults = ruleValidator.validateRule(runcard, rules);

        // 將結果進行整合
        String overallResult = ruleValidator.parseResult(ruleResults);

        // 組合回傳物件
        RuncardResponse response = new RuncardResponse();
        response.setRuncardId(runcard.getId());

        List<RuncardResponse.Result> results = getResults(runcard, rules, ruleResults);

        response.setResults(results);
        return response;
    }

    private static List<RuncardResponse.Result> getResults(Runcard runcard, List<Rule> rules, List<String> ruleResults) {
        List<RuncardResponse.Result> results = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++) {
            Rule rule = rules.get(i);
            String result = ruleResults.get(i);

            RuncardResponse.Result responseResult = new RuncardResponse.Result();
            responseResult.setToolId(runcard.getToolId());
            responseResult.setToolGroupName(rule.getGroupName());
            responseResult.setRule(rule.getName());
            responseResult.setResult(result);
            results.add(responseResult);
        }
        return results;
    }
}
