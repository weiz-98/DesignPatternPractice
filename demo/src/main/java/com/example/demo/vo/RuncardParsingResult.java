package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuncardParsingResult extends RuncardRawInfo {
    private Integer arrivalHours;
    private LocalDateTime latestCheckDt;
    private List<OneConditionToolRuleGroupResult> conditions;
    private Boolean hasApproved;
}
