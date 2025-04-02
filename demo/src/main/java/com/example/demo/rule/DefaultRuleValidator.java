package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultRuleValidator implements IRuleValidator {

    private final RuleCheckFactory ruleCheckFactory;

    /**
     * 逐一將每個 group 所 mapping 到的 rule 用 RuleCheckFactory 取得對應 checker 來執行檢查
     */
    @Override
    public List<ResultInfo> validateRule(String cond, RuncardRawInfo runcardRawInfo, List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            log.error("Runcard ID : {} has no rules to validate",
                    (runcardRawInfo != null ? runcardRawInfo.getRuncardId() : "UNKNOWN"));
            return Collections.emptyList();
        }

        List<ResultInfo> results = new ArrayList<>();
        for (Rule rule : rules) {
            try {
                IRuleCheck checker = ruleCheckFactory.getRuleCheck(rule.getRuleType());
                ResultInfo info = checker.check(cond, runcardRawInfo, rule);
                results.add(info);
            } catch (Exception ex) {
                // 如果該 rule 找不到對應的 checker 或執行出錯 => 做個紅燈
                ResultInfo errorInfo = new ResultInfo();
                errorInfo.setRuleType(rule.getRuleType());
                errorInfo.setResult(3);
                errorInfo.setDetail(Collections.singletonMap("error", ex.getMessage()));
                results.add(errorInfo);
            }
        }
        return results;
    }

    /**
     * parseResult:
     * - 同一個 ruleType 可能有多筆 ResultInfo (來自不同 group)
     * - 需要將重複的 ruleType 合併:
     * 1) 取該 ruleType 的最大燈號 (3 > 2 > 1)
     * 2) 將所有 groupName、以及各自 detail 同時放進最後的 detail
     *
     * @param resultInfos 可能包含多個 ruleType，也可能有重複
     * @return 合併過後的多個 ResultInfo (一個 ruleType 只留一筆)
     */
    @Override
    public List<ResultInfo> parseResult(List<ResultInfo> resultInfos) {
        if (resultInfos == null || resultInfos.isEmpty()) {
            return Collections.emptyList();
        }

        // 依 ruleType 分組
        Map<String, List<ResultInfo>> groupedByRuleType = new HashMap<>();
        for (ResultInfo ri : resultInfos) {
            groupedByRuleType.computeIfAbsent(ri.getRuleType(), k -> new ArrayList<>()).add(ri);
        }

        List<ResultInfo> consolidatedList = new ArrayList<>();
        for (Map.Entry<String, List<ResultInfo>> entry : groupedByRuleType.entrySet()) {
            String ruleType = entry.getKey();
            List<ResultInfo> infosOfSameRule = entry.getValue();

            // 取最大燈號
            // 若全 null 就預設 3(紅燈)，並印 log.error
            Integer maxResult = infosOfSameRule.stream()
                    .map(ResultInfo::getResult)
                    .filter(Objects::nonNull)
                    .max(Integer::compareTo)
                    .orElseGet(() -> {
                        log.error("All results are null for ruleType={}. Defaulting to 3 (red)", ruleType);
                        return 3;
                    });

            //    - 記錄有哪些 group => "repeatedGroups": [groupA, groupB, ...]
            Set<String> groupNames = new HashSet<>();
            Map<String, Object> mergedDetail = new HashMap<>();

            for (ResultInfo ri : infosOfSameRule) {
                Map<String, Object> detail = ri.getDetail();
                if (detail != null) {

                    // groupObj -> string
                    String groupStr = detail.get("group") instanceof String
                            ? (String) detail.get("group")
                            : null;
                    if (groupStr != null) {
                        groupNames.add(groupStr);
                    }

                    // 將其他 detail 加入 mergedDetail，標註出是該 group 的
                    // 例如: groupName_detailKey => detailValue
                    for (Map.Entry<String, Object> me : detail.entrySet()) {
                        String dKey = me.getKey();
                        if ("group".equals(dKey)) {
                            continue;
                        }
                        // 讓 key 看起來像: "groupA_someKey"
                        String finalKey = (groupStr != null)
                                ? (groupStr + "_" + dKey)
                                : dKey;

                        mergedDetail.put(finalKey, me.getValue());
                    }
                }
            }

            if (!groupNames.isEmpty()) {
                mergedDetail.put("repeatedGroups", new ArrayList<>(groupNames));
            }
            logConsolidationDetails(infosOfSameRule, ruleType, groupNames, maxResult);
            // 4. 建立合併後的 ResultInfo
            ResultInfo finalRi = new ResultInfo();
            finalRi.setRuleType(ruleType);
            finalRi.setResult(maxResult);
            finalRi.setDetail(mergedDetail);

            consolidatedList.add(finalRi);
        }

        return consolidatedList;
    }

    private void logConsolidationDetails(List<ResultInfo> infosOfSameRule, String ruleType, Set<String> groupNames, int maxResult) {
        // 嘗試從第一筆 detail 取得 runcardId 與 condition（如果有）
        String runcardId = "UNKNOWN";
        String condition = "UNKNOWN";
        if (!infosOfSameRule.isEmpty()) {
            Map<String, Object> d = infosOfSameRule.getFirst().getDetail();
            if (d != null) {
                if (d.containsKey("runcardId")) {
                    runcardId = d.get("runcardId").toString();
                }
                if (d.containsKey("condition")) {
                    condition = d.get("condition").toString();
                }
            }
        }
        log.info("RuncardID: {} Condition: {} - Consolidating ruleType '{}' from groups {} with individual results: {}. Final result: {}",
                runcardId, condition, ruleType, groupNames, infosOfSameRule, maxResult);
    }
}

