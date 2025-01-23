package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.example.demo.vo.RuncardResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RunCardParserService {

    public RuncardResponse parse(Runcard runcard, List<Rule> rules) {
        RuncardResponse response = new RuncardResponse();
        response.setRuncardId(runcard.getId());

        List<RuncardResponse.Result> results = new ArrayList<>();
        for (Rule rule : rules) {
            RuncardResponse.Result result = new RuncardResponse.Result();
            result.setToolId(runcard.getToolId());
            result.setToolGroupName(rule.getGroupName());
            result.setRule(rule.getName());
            result.setResult("pass"); // 模擬結果
            results.add(result);
        }
        response.setResults(results);
        return response;
    }
}
