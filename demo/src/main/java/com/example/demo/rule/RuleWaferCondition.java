package com.example.demo.rule;

import com.example.demo.po.WaferCondition;
import com.example.demo.service.BatchCache;
import com.example.demo.utils.PreCheckUtil;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component("WaferCondition")
@RequiredArgsConstructor
public class RuleWaferCondition implements IRuleCheck {

    @Override
    public ResultInfo check(RuleExecutionContext ruleExecutionContext, Rule rule) {
        BatchCache cache = ruleExecutionContext.getCache();
        RuncardRawInfo runcardRawInfo = ruleExecutionContext.getRuncardRawInfo();
        String cond = ruleExecutionContext.getCond();
        RecipeToolPair recipeToolPair = ruleExecutionContext.getRecipeToolPair();
        log.info("RuncardID: {} Condition: {} - WaferCondition check start", runcardRawInfo.getRuncardId(), cond);

        ResultInfo pre = PreCheckUtil.run(EnumSet.of(PreCheckType.LOT_TYPE_EMPTY, PreCheckType.LOT_TYPE_MISMATCH),
                cond, runcardRawInfo, rule, recipeToolPair);
        if (pre != null) {
            return pre;
        }

        WaferCondition wc = cache.getWaferCondition(runcardRawInfo.getRuncardId());
        if (wc == null) {
            log.info("RuncardID: {} Condition: {} - No WaferCondition data => skip", runcardRawInfo.getRuncardId(), cond);
            return RuleUtil.buildSkipInfo(rule.getRuleType(), runcardRawInfo, cond, rule, recipeToolPair, 3, "error", "No WaferCondition data => skip", false);
        }
        log.info("RuncardID: {} Condition: {} - WaferCondition retrieved data: {}", runcardRawInfo.getRuncardId(), cond, wc);

        int uniqueCount = RuleUtil.parseIntSafe(wc.getUniqueCount());
        int wfrQty = RuleUtil.parseIntSafe(wc.getWfrQty());

        boolean isEqual = (uniqueCount == wfrQty);
        Lamp lamp = isEqual ? Lamp.GREEN : Lamp.RED;

        log.info("RuncardID: {} Condition: {} - WaferCondition check => uniqueCount = '{}', wfrQty = '{}'", runcardRawInfo.getRuncardId(), cond, uniqueCount, wfrQty);

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeToolPair.getRecipeId());
        detailMap.put("toolIds", recipeToolPair.getToolIds());
        detailMap.put("result", lamp.code());
        detailMap.put("waferCondition", isEqual);
        detailMap.put("wfrQty", wfrQty);
        detailMap.put("experimentQty", uniqueCount);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        log.info("RuncardID: {} Condition: {} - WaferCondition detail = {}", runcardRawInfo.getRuncardId(), cond, detailMap);
        log.info("RuncardID: {} Condition: {} - WaferCondition check done, lamp = '{}'", runcardRawInfo.getRuncardId(), cond, lamp);

        return ResultInfo.builder()
                .ruleType(rule.getRuleType())
                .result(lamp.code())
                .detail(detailMap)
                .build();
    }
}
