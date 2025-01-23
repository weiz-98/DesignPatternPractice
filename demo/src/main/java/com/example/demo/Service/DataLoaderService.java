package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.example.demo.vo.ToolGroup;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DataLoaderService {

    public Map<Runcard, List<Rule>> loadRuncardRules(List<Runcard> runcards, List<ToolGroup> toolGroups) {
        Map<Runcard, List<Rule>> runcardRuleMap = new HashMap<>();
        for (Runcard runcard : runcards) {
            for (ToolGroup group : toolGroups) {
                if (group.getToolList().contains(runcard.getToolId())) {
                    runcardRuleMap.put(runcard, group.getRules());
                }
            }
        }
        return runcardRuleMap;
    }
}

