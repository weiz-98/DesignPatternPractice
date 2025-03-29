package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuncardMappingInfo {
    private RuncardRawInfo runcardRawInfo;                  // 對應 原始 runcard data
    private List<ToolRuleMappingInfo> conditionToolRuleMappingInfos;  // 對應多個 condition
}

