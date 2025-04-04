package com.example.demo.service;

import com.example.demo.rule.DefaultRuleValidator;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class RunCardParserService {

    private final DefaultRuleValidator ruleValidator;

    /**
     * 解析一張 runcard 下所有 condition 的判斷結果
     */
    public List<OneConditionToolRuleGroupResult> validateMappingRules(RuncardMappingInfo runcardMappingInfo) {

        List<OneConditionToolRuleGroupResult> emptyCheckResult = checkBasicParams(runcardMappingInfo);
        if (emptyCheckResult != null) {
            return emptyCheckResult;
        }

        List<OneConditionToolRuleGroupResult> allConditionsResultList = new ArrayList<>();

        RuncardRawInfo runcardRawInfo = runcardMappingInfo.getRuncardRawInfo();

        for (OneConditionToolRuleMappingInfo oneCondMappingInfo : runcardMappingInfo.getOneConditionToolRuleMappingInfos()) {

            // 收集單一 oneConditionToolRuleMappingInfo 的判斷結果
            List<ResultInfo> oneConditionAllResultInfos = new ArrayList<>();

            Map<String, List<Rule>> groupRulesMap = oneCondMappingInfo.getGroupRulesMap();

            //  若沒有 mapping 到任何 group -> 也要產生一筆記錄，表示該 condition 沒有對應到 group
            if (groupRulesMap == null || groupRulesMap.isEmpty()) {
                log.info("RuncardID: {} Condition: {} - No group mapping found",
                        runcardRawInfo.getRuncardId(), oneCondMappingInfo.getCondition());
                ResultInfo noGroupInfo = new ResultInfo();
                noGroupInfo.setRuleType("no-group");
                noGroupInfo.setResult(0); // 0 代表沒有對應
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("msg", "No group matched for this condition");
                noGroupInfo.setDetail(detailMap);
                oneConditionAllResultInfos.add(noGroupInfo);
            } else {
                // 若有 group => 逐一 group 去做 validateRule
                for (Map.Entry<String, List<Rule>> entry : groupRulesMap.entrySet()) {
                    String groupName = entry.getKey();
                    List<Rule> rules = entry.getValue();

                    log.info("RuncardID: {} Condition: {} - Processing group '{}' with rules: {}",
                            runcardRawInfo.getRuncardId(), oneCondMappingInfo.getCondition(), groupName, rules);

                    // 呼叫 DefaultRuleValidator.validateRule 去執行該 condition 下 同 rule 的檢查
                    List<ResultInfo> partialResults = ruleValidator.validateRule(oneCondMappingInfo.getCondition(), runcardRawInfo, rules);

                    // 在 parseResult 階段會把這些 groupName 合併成 "repeatedGroups",
                    // 並在 detail 裡分別以 "GroupA_xxx", "GroupB_xxx" 形式呈現
                    partialResults.forEach(res -> {
                        Map<String, Object> detailMap = (res.getDetail() != null)
                                ? new HashMap<>(res.getDetail())
                                : new HashMap<>();
                        // 讓 detailMap 裡包含 "group" => groupName
                        detailMap.put("group", groupName);
                        res.setDetail(detailMap);
                    });

                    log.info("RuncardID: {} Condition: {} - Group '{}' partial results: {}",
                            runcardRawInfo.getRuncardId(), oneCondMappingInfo.getCondition(), groupName, partialResults);
                    oneConditionAllResultInfos.addAll(partialResults);
                }
            }
            // 進行「同 ruleType 合併 + 取最大燈號 + 累積 detail」
            List<ResultInfo> finalConsolidated = ruleValidator.parseResult(oneConditionAllResultInfos);

            // "toolChambers" 只顯示本 condition 下的 toolChambers
            OneConditionToolRuleGroupResult oneConditionResult = new OneConditionToolRuleGroupResult();
            oneConditionResult.setCondition(oneCondMappingInfo.getCondition());
            oneConditionResult.setToolChambers(oneCondMappingInfo.getToolChambers());
            oneConditionResult.setResults(finalConsolidated);

            log.info("RuncardID: {} Condition: {} - Final consolidated results: {}",
                    runcardRawInfo.getRuncardId(), oneCondMappingInfo.getCondition(), finalConsolidated);
            allConditionsResultList.add(oneConditionResult);
        }

        log.info("RuncardID: {} TotalConditions: {} FinalResultCount: {}",
                runcardRawInfo.getRuncardId(),
                runcardMappingInfo.getOneConditionToolRuleMappingInfos().size(),
                allConditionsResultList.size());

        return allConditionsResultList;
    }

    private List<OneConditionToolRuleGroupResult> checkBasicParams(RuncardMappingInfo runcardMappingInfo) {
        if (runcardMappingInfo == null
                || runcardMappingInfo.getOneConditionToolRuleMappingInfos() == null
                || runcardMappingInfo.getOneConditionToolRuleMappingInfos().isEmpty()) {
            log.error("RunCardParserService.validateMappingRules] No RuncardMappingInfo or no conditions found.");
            return Collections.emptyList();
        }

        RuncardRawInfo runcardRawInfo = runcardMappingInfo.getRuncardRawInfo();
        if (runcardRawInfo == null) {
            log.error("[RunCardParserService.validateMappingRules] RuncardRawInfo is null. Cannot proceed!");
            return Collections.emptyList();
        }
        String runcardId = (runcardRawInfo.getRuncardId() != null)
                ? runcardRawInfo.getRuncardId()
                : "UNKNOWN";
        if ("UNKNOWN".equals(runcardId)) {
            log.error("[RunCardParserService.validateMappingRules] runcardId is UNKNOWN. Cannot proceed!");
            return Collections.emptyList();
        }
        return null;
    }
}

