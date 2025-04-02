package com.example.demo.rule;

import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class RuleRCOwner implements IRuleCheck {

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - Start RuleRCOwner check",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        if (RuleUtil.isLotTypeEmpty(rule)) {
            log.info("RuncardID: {} Condition: {} - lotType is empty => skip check",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(0);
            Map<String, Object> detail = Map.of(
                    "msg", "lotType is empty => skip check",
                    "runcardId", runcardRawInfo.getRuncardId(),
                    "condition", cond
            );
            info.setDetail(detail);
            return info;
        }

        if (RuleUtil.isLotTypeMismatch(runcardRawInfo, rule)) {
            log.info("RuncardID: {} Condition: {} - lotType mismatch => skip check",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(0);
            Map<String, Object> detail = Map.of(
                    "msg", "lotType mismatch => skip check",
                    "runcardId", runcardRawInfo.getRuncardId(),
                    "condition", cond
            );
            info.setDetail(detail);
            return info;
        }

        Map<String, Object> settings = rule.getSettings();
        if (settings == null) {
            log.info("RuncardID: {} Condition: {} - No settings => skip check",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(0);
            Map<String, Object> detail = Map.of(
                    "msg", "No settings => skip check",
                    "runcardId", runcardRawInfo.getRuncardId(),
                    "condition", cond
            );
            info.setDetail(detail);
            return info;
        }

        // names
        Map<String, String> namesMap = RuleUtil.parseStringMap(settings.get("names"));
        List<String> employeeIds = new ArrayList<>(namesMap.values());
        List<String> employeeNames = new ArrayList<>(namesMap.keySet());

        String issuingEngineer = runcardRawInfo.getIssuingEngineer();
        boolean found = employeeIds.contains(issuingEngineer);
        int lamp = found ? 2 : 1;

        log.info("RuncardID: {} Condition: {} - RCOwner check => found={}, finalLamp={}",
                runcardRawInfo.getRuncardId(), cond, found, lamp);

        // sections
        Map<String, String> sectionsMap = RuleUtil.parseStringMap(settings.get("sections"));
        List<String> sectionNames = new ArrayList<>(sectionsMap.keySet());

        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "issuingEngineer", issuingEngineer,
                "configuredRCOwnerOrg", sectionNames,
                "configuredRCOwnerEmployeeId", employeeIds,
                "configuredRCOwnerName", employeeNames,
                "runcardId", runcardRawInfo.getRuncardId(),
                "condition", cond
        );

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RuleRCOwner done, lamp={}",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }

}
