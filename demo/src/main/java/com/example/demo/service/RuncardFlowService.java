package com.example.demo.service;

import com.example.demo.po.ArrivalStatus;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuncardFlowService {

    private final DataLoaderService dataLoaderService;
    private final RuncardHandlerService runcardHandlerService;
    private final RunCardParserService runCardParserService;

    public void processRuncardBatch(RuncardParsingRequest runcardParsingRequest) {
        List<String> sectionIds = runcardParsingRequest.getSectionIds();
        LocalDateTime startTime = runcardParsingRequest.getStartTime();
        LocalDateTime endTime = runcardParsingRequest.getEndTime();

        List<ToolRuleGroup> toolRuleGroups = dataLoaderService.getToolRuleGroups(sectionIds);
        log.info("Retrieved {} ToolRuleGroups for sections : {} between {} and {}", toolRuleGroups.size(), sectionIds, startTime, endTime);

        Optional<List<RuncardRawInfo>> optionalRuncardRawInfos = dataLoaderService.getQueryRuncardBatch(sectionIds, startTime, endTime);

        if (optionalRuncardRawInfos.isEmpty()) {
            log.info("No Runcard found in {} between {} and {}", sectionIds, startTime, endTime);
            return;
        }
        List<RuncardRawInfo> runcardRawInfos = optionalRuncardRawInfos.get();

        log.info("Retrieved {} Runcard Raw Data for sections : {} between {} and {}", runcardRawInfos.size(), sectionIds, startTime, endTime);

        // 建立該 module 下所有 Runcard mapping 到的所有 rules
        List<RuncardMappingInfo> oneModuleMappingInfos = getConditionMappingInfos(runcardRawInfos, toolRuleGroups);

        List<OneRuncardRuleResult> oneModuleRuleResult = processMappingInfos(oneModuleMappingInfos);

        saveOneModuleRuleResult(oneModuleRuleResult);
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

    public void saveOneModuleRuleResult(List<OneRuncardRuleResult> oneModuleRuleResult) {
        List<String> runCardList = oneModuleRuleResult.stream()
                .map(OneRuncardRuleResult::getRuncardId)
                .toList();
        List<ArrivalStatus> arrivalStatuses = dataLoaderService.getRuncardArrivalStatuses(runCardList);
        Map<String, Integer> runCardArrivalMap = arrivalStatuses.stream()
                .collect(Collectors.toMap(ArrivalStatus::getRuncardId, arrivalStatus -> Integer.parseInt(arrivalStatus.getArrivalTime())));

        oneModuleRuleResult.forEach(oneRuncardRuleResult -> {
            String runcardId = oneRuncardRuleResult.getRuncardId();
            // -1 代表未到站
            int arrivalHours = runCardArrivalMap.getOrDefault(runcardId, -1);
            log.info("RuncardID: {} arrivalHours {}", runcardId, arrivalHours);

            // save mongo db
        });
    }
}

