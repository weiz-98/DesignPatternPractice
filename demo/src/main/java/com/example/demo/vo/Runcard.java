package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Runcard {
    private String id;           // Runcard ID
    private String toolId;       // 所屬 Tool ID
    private String createTime;   // 開立時間
    private String approver;     // 簽核者
}

