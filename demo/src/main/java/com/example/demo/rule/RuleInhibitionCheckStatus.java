package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.service.BatchCache;
import com.example.demo.utils.PreCheckUtil;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component("InhibitionCheckStatus")
@RequiredArgsConstructor
public class RuleInhibitionCheckStatus implements IRuleCheck {


    @Override
    public ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule) {
        BatchCache cache = ruleExecutionContext.getCache();
        RuncardRawInfo runcardRawInfo = ruleExecutionContext.getRuncardRawInfo();
        String cond = ruleExecutionContext.getCond();
        RecipeToolPair recipeToolPair = ruleExecutionContext.getRecipeToolPair();
        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check start", runcardRawInfo.getRuncardId(), cond);

        ResultInfo pre = PreCheckUtil.run(EnumSet.of(PreCheckType.LOT_TYPE_EMPTY, PreCheckType.LOT_TYPE_MISMATCH),
                cond, runcardRawInfo, rule, recipeToolPair);
        if (pre != null) {
            return pre;
        }

        List<InhibitionCheckStatus> list = cache.getInhibitionCheckStatus(runcardRawInfo.getRuncardId());
        if (list.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No InhibitionCheckStatus data => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No InhibitionCheckStatus data => skip", false);
        }
        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus retrieved {} rows", runcardRawInfo.getRuncardId(), cond, list.size());

        String baseCond = cond.contains("_M") ? cond.split("_M", 2)[0] : cond;

        InhibitionCheckStatus target = list.stream()
                .filter(ics -> baseCond.equals(ics.getCondition()))
                .findFirst()
                .orElse(null);

        if (target == null) {
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No InhibitionCheckStatus for condition", false);
        }

        boolean flagY = "Y".equalsIgnoreCase(target.getInhibitFlag());
        Lamp lamp = flagY ? Lamp.GREEN : Lamp.YELLOW;

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check => inhibitFlag='{}'", runcardRawInfo.getRuncardId(), cond, target.getInhibitFlag());

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
        detailMap.put("result", lamp.code());
        detailMap.put("inhibitionCheck", flagY);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus detail = {}", runcardRawInfo.getRuncardId(), cond, detailMap);
        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check done, lamp = '{}'", runcardRawInfo.getRuncardId(), cond, lamp);

        return ResultInfo.builder()
                .ruleType(rule.getRuleType())
                .result(lamp.code())
                .detail(detailMap)
                .build();
    }
}
