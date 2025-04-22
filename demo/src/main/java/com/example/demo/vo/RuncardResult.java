package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuncardResult {
    private String runcardId;
    private String approver;
    private Integer arrivalHours;
    private LocalDateTime latestCheckDt;
    private List<OneConditionToolRuleGroupResult> conditions;
    private Boolean hasApproved;
}
