package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleInhibitionCheckStatus implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check start",
                runcardRawInfo.getRuncardId(), cond);

        String recipeId = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId())
                .stream()
                .filter(o -> cond.contains("_M") ? cond.startsWith(o.getCondition())
                        : cond.equals(o.getCondition()))
                .map(OneConditionRecipeAndToolInfo::getRecipeId)
                .findFirst()
                .orElse("");

        if (cond.contains("_M")) {
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeId, 0,
                    "msg", "Skip M-Condition", true);
        }

        ResultInfo r = RuleUtil.addRecipe(RuleUtil.checkLotTypeEmpty(cond, runcardRawInfo, rule), recipeId);
        if (r != null) return r;
        r = RuleUtil.addRecipe(RuleUtil.checkLotTypeMismatch(cond, runcardRawInfo, rule), recipeId);
        if (r != null) return r;

        List<InhibitionCheckStatus> list = dataLoaderService.getInhibitionCheckStatus(runcardRawInfo.getRuncardId());
        if (list.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No InhibitionCheckStatus data => skip",
                    runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeId, 3,
                    "error", "No InhibitionCheckStatus data => skip", false);
        }

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus retrieved {} rows",
                runcardRawInfo.getRuncardId(), cond,
                list.size());

        InhibitionCheckStatus target = list.stream()
                .filter(ics -> cond.equals(ics.getCondition()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule,
                    recipeId, 3,
                    "error", "No InhibitionCheckStatus for condition", false);
        }

        boolean flagY = "Y".equalsIgnoreCase(target.getInhibitFlag());
        int lamp = flagY ? 1 : 2;

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check => inhibitFlag='{}'",
                runcardRawInfo.getRuncardId(), cond, target.getInhibitFlag());

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeId);
        detailMap.put("result", lamp);
        detailMap.put("inhibitionCheck", flagY);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());
        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus detail = {}",
                runcardRawInfo.getRuncardId(), cond, detailMap);


        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check done, lamp = '{}'",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }
}
