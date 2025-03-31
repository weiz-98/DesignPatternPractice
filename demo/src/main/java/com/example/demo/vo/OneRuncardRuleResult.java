package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OneRuncardRuleResult {
    private String runcardId;
    private List<OneConditionToolRuleGroupResult> oneConditionToolRuleGroupResults;
}
