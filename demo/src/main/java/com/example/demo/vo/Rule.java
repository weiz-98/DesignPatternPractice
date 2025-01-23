package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Rule {
    private String name;         // Rule 名稱
    private String groupName;    // 所屬 Tool Group 名稱
}

