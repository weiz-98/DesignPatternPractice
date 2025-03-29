package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    private String ruleType;              // 規則類型 (ex: ruleA, ruleB...)
    private List<String> lotType;         // 此規則適用哪些 lot type
    private Map<String, Object> settings; // 其他規則參數或設定值
}


