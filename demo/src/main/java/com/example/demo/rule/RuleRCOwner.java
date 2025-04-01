package com.example.demo.rule;

import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RuleRCOwner implements IRuleCheck {

    @Override
    public ResultInfo check(RuncardRawInfo runcardRawInfo, Rule rule) {
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        if (RuleUtil.isLotTypeEmpty(rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType is empty => skip check"));
            return info;
        }

        if (RuleUtil.shouldCheckLotType(runcardRawInfo, rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType mismatch => skip check"));
            return info;
        }

        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "No settings => skip check"));
            return info;
        }

        Map<String, String> namesMap = parseStringMap(settings.get("names"));
        List<String> employeeIds = new ArrayList<>(namesMap.values());
        List<String> employeeNames = new ArrayList<>(namesMap.keySet());

        String issuingEngineer = runcardRawInfo.getIssuingEngineer();
        boolean found = employeeIds.contains(issuingEngineer);
        int lamp = found ? 2 : 1;

        Map<String, String> sectionsMap = parseStringMap(settings.get("sections"));
        List<String> sectionNames = new ArrayList<>(sectionsMap.keySet());

        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "issuingEngineer", issuingEngineer,
                "configuredRCOwnerOrg", sectionNames,
                "configuredRCOwnerEmployeeId", employeeIds,
                "configuredRCOwnerName", employeeNames
        );

        info.setResult(lamp);
        info.setDetail(detailMap);

        return info;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseStringMap(Object obj) {
        if (obj instanceof Map) {
            try {
                return (Map<String, String>) obj;
            } catch (ClassCastException ex) {
                log.warn("parseStringMap cast fail, obj={}", obj);
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }
}
