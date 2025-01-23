package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.example.demo.vo.RuncardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class RuncardHandlerService {

    private final RunCardParserService runCardParserService;

    public List<RuncardResponse> processRuncards(Map<Runcard, List<Rule>> runcardRuleMap) {
        List<RuncardResponse> responses = new ArrayList<>();
        for (Map.Entry<Runcard, List<Rule>> entry : runcardRuleMap.entrySet()) {
            RuncardResponse response = runCardParserService.parse(entry.getKey(), entry.getValue());
            responses.add(response);
        }
        return responses;
    }
}

