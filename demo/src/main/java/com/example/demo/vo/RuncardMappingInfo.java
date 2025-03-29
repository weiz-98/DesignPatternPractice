package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuncardMappingInfo {
    private RuncardRawInfo runcardRawInfo; // runcard raw data
    private List<OneConditionToolRuleMappingInfo> oneConditionToolRuleMappingInfos; // 對應多個 condition
}

