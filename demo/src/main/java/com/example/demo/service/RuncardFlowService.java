package com.example.demo.service;

import com.example.demo.po.ArrivalStatus;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuncardFlowService {

    private final DataLoaderService dataLoaderService;
    private final RuncardHandlerService runcardHandlerService;
    private final RunCardParserService runCardParserService;

    public List<RuncardParsingResult> processRuncardBatch(RuncardParsingRequest runcardParsingRequest) {
        List<String> sectionIds = runcardParsingRequest.getSectionIds();
        LocalDateTime startTime = runcardParsingRequest.getStartTime();
        LocalDateTime endTime = runcardParsingRequest.getEndTime();

        List<ToolRuleGroup> toolRuleGroups = dataLoaderService.getToolRuleGroups(sectionIds);
        log.info("Retrieved {} ToolRuleGroups for sections : {} between {} and {}", toolRuleGroups.size(), sectionIds, startTime, endTime);

        Optional<List<RuncardRawInfo>> optionalRuncardRawInfos = dataLoaderService.getQueryRuncardBatch(sectionIds, startTime, endTime);

        if (optionalRuncardRawInfos.isEmpty()) {
            log.info("No Runcard found in {} between {} and {}", sectionIds, startTime, endTime);
            return List.of(RuncardParsingResult.builder().build());
        }
        List<RuncardRawInfo> runcardRawInfos = optionalRuncardRawInfos.get();

        log.info("Retrieved {} Runcard Raw Data for sections : {} between {} and {}", runcardRawInfos.size(), sectionIds, startTime, endTime);

        // 建立該 module 下所有 Runcard mapping 到的所有 rules
        List<RuncardMappingInfo> oneModuleMappingInfos = getConditionMappingInfos(runcardRawInfos, toolRuleGroups);

        List<OneRuncardRuleResult> oneModuleRuleResult = processMappingInfos(oneModuleMappingInfos);

        List<RuncardResult> runcardResults = saveOneModuleRuleResult(oneModuleRuleResult);
        return buildParsingResults(runcardRawInfos, runcardResults);

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

    public List<RuncardResult> saveOneModuleRuleResult(List<OneRuncardRuleResult> oneModuleRuleResult) {
        List<String> runCardList = oneModuleRuleResult.stream()
                .map(OneRuncardRuleResult::getRuncardId)
                .toList();
        List<ArrivalStatus> arrivalStatuses = dataLoaderService.getRuncardArrivalStatuses(runCardList);

        Map<String, Double> runCardArrivalMap = arrivalStatuses.stream()
                .collect(Collectors.toMap(
                        ArrivalStatus::getRuncardId,
                        a -> safeParseDouble(a.getArrivalTime(), -1.0)));

        List<RuncardResult> results = new ArrayList<>();

        oneModuleRuleResult.forEach(oneRuncardRuleResult -> {
            String runcardId = oneRuncardRuleResult.getRuncardId();
            // -1 代表未到站
            double arrivalHours = runCardArrivalMap.getOrDefault(runcardId, -1.0);
            log.info("RuncardID: {} arrivalHours {}", runcardId, arrivalHours);

            // save mongo db
            RuncardResult runcardResult = RuncardResult.builder()
                    .runcardId(runcardId)
                    .approver(null)
                    .hasApproved(false)
                    .latestCheckDt(LocalDateTime.now())
                    .arrivalHours(arrivalHours)
                    .conditions(oneRuncardRuleResult.getOneConditionToolRuleGroupResults())
                    .build();

            results.add(runcardResult);
        });
        return results;
    }

    public List<RuncardParsingResult> buildParsingResults(
            List<RuncardRawInfo> rawInfos,
            List<RuncardResult> runcardResults) {


        Map<String, RuncardResult> resultMap =
                runcardResults.stream()
                        .collect(Collectors.toMap(RuncardResult::getRuncardId, r -> r));

        LocalDateTime now = LocalDateTime.now();

        return rawInfos.stream()
                .<RuncardParsingResult>map(raw -> {
                    RuncardResult rr = resultMap.get(raw.getRuncardId());
                    return buildParsing(raw, rr);
                })
                .toList();
    }

    private RuncardParsingResult buildParsing(RuncardRawInfo raw, RuncardResult rr) {

        return RuncardParsingResult.builder()
                .runcardId(raw.getRuncardId())
                .issuingEngineer(raw.getIssuingEngineer())
                .lotId(raw.getLotId())
                .partId(raw.getPartId())
                .status(raw.getStatus())
                .purpose(raw.getPurpose())
                .supervisorAndDepartment(raw.getSupervisorAndDepartment())
                .numberOfPieces(raw.getNumberOfPieces())
                .holdAtOperNo(raw.getHoldAtOperNo())
                .arrivalHours(Optional.ofNullable(rr).map(RuncardResult::getArrivalHours).orElse(-1.0))
                .latestCheckDt(Optional.ofNullable(rr).map(RuncardResult::getLatestCheckDt).orElse(LocalDateTime.now()))
                .conditions(Optional.ofNullable(rr).map(RuncardResult::getConditions).orElse(Collections.emptyList()))
                .hasApproved(Optional.ofNullable(rr).map(RuncardResult::getHasApproved).orElse(false))
                .build();
    }

    private double safeParseDouble(String str, double defaultVal) {
        if (str == null || str.isBlank()) return defaultVal;
        try {
            return Double.parseDouble(str.trim());
        } catch (NumberFormatException ex) {
            log.warn("[safeParseDouble] cannot parse '{}', use default {}", str, defaultVal);
            return defaultVal;
        }
    }

}

