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
        log.info("[RuncardFlowService.process] Starting cron job at: {}", LocalDateTime.now());

        // 從 dataLoaderService 拿到所有的 module
        List<ModuleInfo> moduleInfoList = dataLoaderService.getModules();
        log.info("Found {} modules to process.", moduleInfoList.size());

        moduleInfoList.forEach(this::processModule);
    }

    private void processModule(ModuleInfo moduleInfo) {
        log.info("[processModule] Processing module: {}, sections: {}", moduleInfo.getModule(), moduleInfo.getSectionIds());
        List<String> sectionIds = moduleInfo.getSectionIds();

        List<ToolRuleGroup> toolRuleGroups = dataLoaderService.getToolRuleGroups(sectionIds);
        log.info("Retrieved {} ToolRuleGroups for module: {}", toolRuleGroups.size(), moduleInfo.getModule());

        List<RuncardRawInfo> runcardRawInfos = dataLoaderService.getMockRUncardRawInfoList(sectionIds, startTime, endTime);
        log.info("Retrieved {} RuncardRawInfos for module: {}", runcardRawInfos.size(), moduleInfo.getModule());

        // 建立該 module 下所有 Runcard 的 ConditionMappingInfos
        List<RuncardMappingInfo> runcardMappingInfos = getConditionMappingInfos(runcardRawInfos, toolRuleGroups);

        processMappingInfos(runcardMappingInfos);
    }

    private List<RuncardMappingInfo> getConditionMappingInfos(List<RuncardRawInfo> runcardRawInfos, List<ToolRuleGroup> toolRuleGroups) {
        List<RuncardMappingInfo> moduleConditionMappingInfos = new ArrayList<>();
        for (RuncardRawInfo runcardRawInfo : runcardRawInfos) {
            List<RecipeAndToolInfo> recipeAndToolInfos = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId());
            // 取得每一張 runcard 下每個 condition 所對應的 rule 資訊
            RuncardMappingInfo conditionMappingInfo = runcardHandlerService.buildConditionMappingInfo(runcardRawInfo, recipeAndToolInfos, toolRuleGroups);
            moduleConditionMappingInfos.add(conditionMappingInfo);
        }
        return moduleConditionMappingInfos;
    }


    private void processMappingInfos(List<RuncardMappingInfo> runcardMappingInfos) {
        // 將每一張 runcard 的 runcardMappingInfo 根據不同的 rule 去做驗證
        runcardMappingInfos.forEach(runcardMappingInfo -> {
            List<ToolRuleGroupResult> ruleResults = runCardParserService.validateMappingRules(runcardMappingInfo);

            RuncardRawInfo rawInfo = runcardMappingInfo.getRuncardRawInfo();
            log.info("[processMappingInfos] RuncardID={} validated. Result size={} ",
                    rawInfo.getRuncardId(), ruleResults.size());
        });
    }
}

