package com.example.demo.rule;

import com.example.demo.po.IssuingEngineerInfo;
import com.example.demo.service.BatchCache;
import com.example.demo.utils.PreCheckUtil;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("RCOwner")
@RequiredArgsConstructor
public class RuleRCOwner implements IRuleCheck {


    @Override
    public ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule) {
        BatchCache cache = ruleExecutionContext.getCache();
        RuncardRawInfo runcardRawInfo = ruleExecutionContext.getRuncardRawInfo();
        String cond = ruleExecutionContext.getCond();
        RecipeToolPair recipeToolPair = ruleExecutionContext.getRecipeToolPair();
        log.info("RuncardID: {} Condition: {} - RCOwner check start", runcardRawInfo.getRuncardId(), cond);

        ResultInfo pre = PreCheckUtil.run(EnumSet.of(PreCheckType.LOT_TYPE_EMPTY, PreCheckType.LOT_TYPE_MISMATCH, PreCheckType.SETTINGS_NULL),
                cond, runcardRawInfo, rule, recipeToolPair);
        if (pre != null) {
            return pre;
        }

        Map<String, Object> settings = rule.getSettings();

        List<String> divisions = RuleUtil.parseStringList(settings.get("divisions"));
        List<String> departments = RuleUtil.parseStringList(settings.get("departments"));
        List<String> sections = RuleUtil.parseStringList(settings.get("sections"));
        List<String> employees = RuleUtil.parseStringList(settings.get("employees"));
        log.info("RuncardID: {} Condition: {} - RCOwner configured => divisions={}, departments={} sections={}, employees={},", runcardRawInfo.getRuncardId(), cond, divisions, departments, sections, employees);

        Optional<String> empIdOpt = extractEmpId(runcardRawInfo.getIssuingEngineer());
        if (empIdOpt.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - issuingEngineer format unexpected => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeToolPair, 3, "error", "issuingEngineer format unexpected (empId not found) => skip", false);
        }
        String empId = empIdOpt.get();

        List<IssuingEngineerInfo> issuingEngineerInfos = cache.getIssuingEngineerInfo(List.of(empId));
        if (issuingEngineerInfos.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No IssuingEngineerInfos data => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No IssuingEngineerInfos data => skip", false);
        }

        IssuingEngineerInfo matchedEngineer = issuingEngineerInfos.stream()
                .filter(engineerInfo -> empId.equals(engineerInfo.getEngineerId()))
                .findFirst()
                .orElse(null);

        if (matchedEngineer == null) {
            log.info("RuncardID: {} Condition: {} - No matching IssuingEngineerInfo data => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeToolPair, 3, "error", "No matching IssuingEngineerInfo data => skip", false);
        }

        boolean divisionMatch = divisions.contains(matchedEngineer.getDivisionId());
        boolean departmentMatch = departments.contains(matchedEngineer.getDepartmentId());
        boolean sectionMatch = sections.contains(matchedEngineer.getSectionId());
        boolean employeeMatch = employees.contains(matchedEngineer.getEngineerId());

        boolean found = divisionMatch || departmentMatch || sectionMatch || employeeMatch;
        Lamp lamp = found ? Lamp.YELLOW : Lamp.GREEN;

        log.info("RuncardID: {} Condition: {} - RCOwner check => divisionMatch = '{}', departmentMatch = '{}', sectionMatch = '{}', employeeMatch = '{}',", runcardRawInfo.getRuncardId(), cond, divisionMatch, departmentMatch, sectionMatch, employeeMatch);

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
        detailMap.put("result", lamp.code());
        detailMap.put("issuingEngineer", runcardRawInfo.getIssuingEngineer());
        detailMap.put("configuredDivisionsIds", divisions);
        detailMap.put("configuredDepartmentIds", departments);
        detailMap.put("configuredSectionsIds", sections);
        detailMap.put("configuredEmployeesIds", employees);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        log.info("RuncardID: {} Condition: {} - RCOwner detail = {}", runcardRawInfo.getRuncardId(), cond, detailMap);
        log.info("RuncardID: {} Condition: {} - RCOwner check done, lamp = '{}'", runcardRawInfo.getRuncardId(), cond, lamp);

        return ResultInfo.builder()
                .ruleType(rule.getRuleType())
                .result(lamp.code())
                .detail(detailMap)
                .build();
    }

    private Optional<String> extractEmpId(String issuingEngineer) {
        if (issuingEngineer == null || issuingEngineer.isBlank()) {
            return Optional.empty();
        }
        String[] parts = issuingEngineer.split("/", 3);
        return (parts.length > 2 && !parts[1].isBlank())
                ? Optional.of(parts[1].trim())
                : Optional.empty();
    }
}
