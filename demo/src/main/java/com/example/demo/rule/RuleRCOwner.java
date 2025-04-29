package com.example.demo.rule;

import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleRCOwner implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - RCOwner check start",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        ResultInfo r;
        r = RuleUtil.checkLotTypeEmpty(cond, runcardRawInfo, rule);
        if (r != null) return r;
        r = RuleUtil.checkLotTypeMismatch(cond, runcardRawInfo, rule);
        if (r != null) return r;
        r = RuleUtil.checkSettingsNull(cond, runcardRawInfo, rule);
        if (r != null) return r;

        Map<String, Object> settings = rule.getSettings();
        // names
        Map<String, String> namesMap = RuleUtil.parseStringMap(settings.get("names"));
        List<String> employeeIds = new ArrayList<>(namesMap.values());
        List<String> employeeNames = new ArrayList<>(namesMap.keySet());
        log.info("RuncardID: {} Condition: {} - RCOwner configured => employeeIds={}, employeeNames={}",
                runcardRawInfo.getRuncardId(), cond, employeeIds, employeeNames);

        // issuingEngineer，格式可能為 "Dept/EmpId/EmpName"
        String engineerName = runcardRawInfo.getIssuingEngineer();
        if (engineerName != null && engineerName.contains("/")) {
            engineerName = engineerName.substring(engineerName.lastIndexOf('/') + 1).trim();
        }

        boolean found = employeeNames.contains(engineerName);
        int lamp = found ? 2 : 1;

        log.info("RuncardID: {} Condition: {} - RCOwner check => found = '{}'",
                runcardRawInfo.getRuncardId(), cond, found);

        // sections
        Map<String, String> sectionsMap = RuleUtil.parseStringMap(settings.get("sections"));
        List<String> sectionNames = new ArrayList<>(sectionsMap.keySet());

        String recipeId = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId())
                .stream()
                .filter(o -> cond.equals(o.getCondition()))
                .map(OneConditionRecipeAndToolInfo::getRecipeId)
                .findFirst()
                .orElse("");

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeId);
        detailMap.put("result", lamp);
        detailMap.put("issuingEngineer", engineerName);
        detailMap.put("configuredRCOwnerOrg", sectionNames);
        detailMap.put("configuredRCOwnerEmployeeId", employeeIds);
        detailMap.put("configuredRCOwnerName", employeeNames);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RCOwner detail = {}",
                runcardRawInfo.getRuncardId(), cond, detailMap);

        log.info("RuncardID: {} Condition: {} - RCOwner check done, lamp = '{}'",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }

}
