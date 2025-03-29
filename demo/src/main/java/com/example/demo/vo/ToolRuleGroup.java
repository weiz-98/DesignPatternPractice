package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolRuleGroup {
    private String groupName;
    private String updateUser;
    private String module;
    private String department;
    private String section;
    private LocalDateTime updateDt;
    private List<ToolInfo> tools;
    private List<Rule> rules;
}


