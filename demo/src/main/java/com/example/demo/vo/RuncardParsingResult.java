package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.List;


@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class RuncardParsingResult extends RuncardRawInfo {
    private Integer arrivalHours;
    private LocalDateTime latestCheckDt;
    private List<OneConditionToolRuleGroupResult> conditions;
    private Boolean hasApproved;
}
