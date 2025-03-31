package com.example.demo.Service;

import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuncardFlowService {

    private final DataLoaderService dataLoaderService;
    private final RuncardHandlerService runcardHandlerService;
    private final RunCardParserService runCardParserService;
    private final LocalDateTime startTime = LocalDateTime.now();
    private final LocalDateTime endTime = startTime.minusWeeks(2);

    public void process() {
        log.info("Starting cron job at :{}", LocalDateTime.now());

        List<ModuleInfo> moduleInfoList = dataLoaderService.getModules();
        log.info("Found {} modules to process.", moduleInfoList.size());

        moduleInfoList.forEach(this::processModule);
    }

    public void processModule(ModuleInfo moduleInfo) {
        log.info("Processing module: {}, sections: {}", moduleInfo.getModule(), moduleInfo.getSectionIds());
        List<String> sectionIds = moduleInfo.getSectionIds();

        List<ToolRuleGroup> toolRuleGroups = dataLoaderService.getToolRuleGroups(sectionIds);
        log.info("Retrieved {} ToolRuleGroups for module: {}", toolRuleGroups.size(), moduleInfo.getModule());

        List<RuncardRawInfo> runcardRawInfos = dataLoaderService.getMockRUncardRawInfoList(sectionIds, startTime, endTime);
        log.info("Retrieved {} Runcard Raw Data for module: {}", runcardRawInfos.size(), moduleInfo.getModule());

        // 建立該 module 下所有 Runcard mapping 到的所有 rules
        List<RuncardMappingInfo> oneModuleMappingInfos = getConditionMappingInfos(runcardRawInfos, toolRuleGroups);

        List<OneRuncardRuleResult> oneModuleRuleResult = processMappingInfos(oneModuleMappingInfos);
    }

    public List<RuncardMappingInfo> getConditionMappingInfos(List<RuncardRawInfo> runcardRawInfos, List<ToolRuleGroup> toolRuleGroups) {
        List<RuncardMappingInfo> oneModuleMappingInfos = new ArrayList<>();
        for (RuncardRawInfo runcardRawInfo : runcardRawInfos) {
            List<OneConditionRecipeAndToolInfo> oneRuncardRecipeAndToolInfos = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId());

            RuncardMappingInfo oneRuncardConditionMappingInfo = runcardHandlerService.buildRuncardMappingInfo(runcardRawInfo, oneRuncardRecipeAndToolInfos, toolRuleGroups);
            oneModuleMappingInfos.add(oneRuncardConditionMappingInfo);
        }
        return oneModuleMappingInfos;
    }


    public List<OneRuncardRuleResult> processMappingInfos(List<RuncardMappingInfo> oneModuleMappingInfos) {
        List<OneRuncardRuleResult> oneModuleRuleResult = new ArrayList<>();
        // 將每一張 runcard 的 runcardMappingInfo 根據不同的 rule 去做驗證
        oneModuleMappingInfos.forEach(oneRuncardMappingInfo -> {
            List<OneConditionToolRuleGroupResult> oneRuncardRuleResults = runCardParserService.validateMappingRules(oneRuncardMappingInfo);

            RuncardRawInfo runcardRawInfo = oneRuncardMappingInfo.getRuncardRawInfo();
            log.info("RuncardID: {} validation completed. Result size : {} ",
                    runcardRawInfo.getRuncardId(), oneRuncardRuleResults.size());
            oneModuleRuleResult.add(OneRuncardRuleResult.builder()
                    .runcardId(runcardRawInfo.getRuncardId())
                    .oneConditionToolRuleGroupResults(oneRuncardRuleResults)
                    .build());
        });
        return oneModuleRuleResult;
    }
}

