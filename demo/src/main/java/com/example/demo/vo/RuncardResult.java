package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuncardResult {
    private String runcardId;                // 對應 Runcard 裡的 runcardId
    private String approver;                 // 最終審批者
    private LocalDateTime latestCheckDt;     // 最新檢查時間
    private List<OneConditionToolRuleGroupResult> conditions;  // 每個 condition（或組）的結果
    private Boolean hasApproved;             // 是否審批通過
}

