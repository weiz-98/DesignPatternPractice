package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolRuleGroupResult {

    private Map<String, List<String>> toolChambers; // key = conditionName, value = List of "tool#chamber"

    private List<ResultInfo> results;    // 針對每條 rule 的判斷結果
}

