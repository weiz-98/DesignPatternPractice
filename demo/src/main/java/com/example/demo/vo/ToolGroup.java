package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolGroup {
    private String name;         // Tool Group 名稱
    private List<String> toolList; // Tool Group 包含的 Tool ID 清單
    private List<Rule> rules;    // Tool Group 中的 Rules
}

