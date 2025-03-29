package com.example.demo.Service;

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
     *
     * @param runcardMappingInfo RuncardMappingInfo (一張 runcard 下的多個 condition + 原始資料)
     * @return 一組 ToolRuleGroupResult (示範回傳: 只回一筆 groupResult，內含最終合併的 resultInfos)
     */
    public List<ToolRuleGroupResult> validateMappingRules(RuncardMappingInfo runcardMappingInfo) {
        // 呼叫檢查方法, 若檢查結果不為 null => 表示有錯，直接回傳
        List<ToolRuleGroupResult> checkResult = checkBasicParams(runcardMappingInfo);
        if (checkResult != null) {
            return checkResult;  // 為空清單
        }

        // 取得 RuncardRawInfo & runcardId
        RuncardRawInfo runcardRawInfo = runcardMappingInfo.getRuncardRawInfo();
        String runcardId = runcardRawInfo.getRuncardId();

        // 收集所有 condition 的判斷結果
        List<ResultInfo> allResultInfos = new ArrayList<>();

        // 逐一 condition
        for (ToolRuleMappingInfo condition : runcardMappingInfo.getConditionToolRuleMappingInfos()) {
            // 取出每個 groupName -> List<Rule>
            Map<String, List<Rule>> groupRulesMap = condition.getGroupRulesMap();

            //     若沒有 mapping 到任何 group -> 也要產生一筆記錄
            //     表示該 condition 沒有對應到 group
            if (groupRulesMap == null || groupRulesMap.isEmpty()) {
                // 直接做一個 resultInfo 來表示 "no group mapped"
                ResultInfo noGroupInfo = new ResultInfo();
                noGroupInfo.setRuleType("no-group");
                noGroupInfo.setResult(0); // 可視需求用 1=綠, 或 0 代表沒有對應?
                Map<String, Object> detailMap = new HashMap<>();
                detailMap.put("msg", "No group matched for this condition");
                noGroupInfo.setDetail(detailMap);
                allResultInfos.add(noGroupInfo);
                continue;
            }

            // 針對同一個 condition 下的每個 group，都要執行 ruleValidator
            for (Map.Entry<String, List<Rule>> entry : groupRulesMap.entrySet()) {
                String groupName = entry.getKey();
                List<Rule> rules = entry.getValue();

                // 呼叫 DefaultRuleValidator.validateRule 去執行工廠模式 + 規則檢查
                List<ResultInfo> partialResults = ruleValidator.validateRule(runcardRawInfo, rules);

                // 在每個 ResultInfo 裡面，額外紀錄該 rule 屬於哪個 group
                partialResults.forEach(res -> {
                    Map<String, Object> detailMap = (res.getDetail() != null)
                            ? new HashMap<>(res.getDetail())
                            : new HashMap<>();
                    // 讓 detailMap 裡包含 "group" => groupName
                    detailMap.put("group", groupName);
                    res.setDetail(detailMap);
                });

                // 加到 allResultInfos
                allResultInfos.addAll(partialResults);
            }
        }


        // 所有判斷都完成後，透過 DefaultRuleValidator.parseResult 進行「同 ruleType 合併 + 取最大燈號 + 累積 detail」
        List<ResultInfo> finalConsolidated = ruleValidator.parseResult(allResultInfos);

        // (1) 新的 VO 結構 => 把所有 condition 的 toolChambers 原樣放在回傳物件中
        Map<String, List<String>> mergedToolChambers = collectAllToolChambers(runcardMappingInfo);

        // 建立最後回傳的 ToolRuleGroupResult
        ToolRuleGroupResult finalGroupResult = new ToolRuleGroupResult();
        finalGroupResult.setToolChambers(mergedToolChambers);      // 把合併的 toolChambers 放進去
        finalGroupResult.setResults(finalConsolidated);

        log.info("[validateMappingRules] runcard={}, conditionsCount={}, finalResultSize={}",
                runcardId,
                runcardMappingInfo.getConditionToolRuleMappingInfos().size(),
                finalConsolidated.size());

        return Collections.singletonList(finalGroupResult);
    }

    private List<ToolRuleGroupResult> checkBasicParams(RuncardMappingInfo runcardMappingInfo) {
        if (runcardMappingInfo == null
                || runcardMappingInfo.getConditionToolRuleMappingInfos() == null
                || runcardMappingInfo.getConditionToolRuleMappingInfos().isEmpty()) {
            log.error("[validateMappingRules] No RuncardMappingInfo or no conditions found.");
            return Collections.emptyList();
        }

        RuncardRawInfo runcardRawInfo = runcardMappingInfo.getRuncardRawInfo();
        if (runcardRawInfo == null) {
            log.error("[validateMappingRules] RuncardRawInfo is null. Cannot proceed!");
            return Collections.emptyList();
        }
        String runcardId = (runcardRawInfo.getRuncardId() != null)
                ? runcardRawInfo.getRuncardId()
                : "UNKNOWN";
        if ("UNKNOWN".equals(runcardId)) {
            log.error("[validateMappingRules] runcardId is UNKNOWN. Cannot proceed!");
            return Collections.emptyList();
        }

        // 檢查都 OK => 回傳 null 表示無需中斷
        return null;
    }

    /**
     * 將所有 condition 的 toolChambers 整合成一個 map (key = conditionString, value = [list of "tool#chamber"])。
     */
    private Map<String, List<String>> collectAllToolChambers(RuncardMappingInfo runcardMappingInfo) {
        Map<String, List<String>> merged = new HashMap<>();

        for (ToolRuleMappingInfo mappingInfo : runcardMappingInfo.getConditionToolRuleMappingInfos()) {
            // 1. 取得 condition 與 toolChambers
            String condKey = mappingInfo.getCondition();
            List<String> chamberList = mappingInfo.getToolChambers();

            // 2. 基本檢查，若有需要可加上 null / isEmpty 判斷
            if (condKey == null || condKey.isEmpty() || chamberList == null || chamberList.isEmpty()) {
                continue;
            }

            // 3. 合併到 merged Map
            merged.computeIfAbsent(condKey, k -> new ArrayList<>()).addAll(chamberList);
        }

        return merged;
    }

}

