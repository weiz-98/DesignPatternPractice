package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OneConditionToolRuleMappingInfo {
    private String condition;
    private List<String> toolChambers;
    private Map<String, List<Rule>> groupRulesMap; // <groupName, List<Rule>>
}

