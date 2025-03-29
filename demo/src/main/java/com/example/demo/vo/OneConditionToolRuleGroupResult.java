package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OneConditionToolRuleGroupResult {
    private String condition;
    private List<String> toolChambers; // key = conditionName, value = List of "tool#chamber"
    private List<ResultInfo> results;    // 針對每條 rule 的判斷結果
}

