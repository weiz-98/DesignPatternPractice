package com.example.demo.rule;

import com.example.demo.po.WaferCondition;
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
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleWaferCondition implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - WaferCondition check start",
                runcardRawInfo.getRuncardId(), cond);

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        String recipeId = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId())
                .stream()
                .filter(o -> {
                    if (cond.contains("_M")) {
                        return cond.startsWith(o.getCondition());
                    }
                    return cond.equals(o.getCondition());
                })
                .map(OneConditionRecipeAndToolInfo::getRecipeId)
                .findFirst()
                .orElse("");

        ResultInfo r = RuleUtil.addRecipe(RuleUtil.checkLotTypeEmpty(cond, runcardRawInfo, rule), recipeId);
        if (r != null) return r;

        r = RuleUtil.addRecipe(RuleUtil.checkLotTypeMismatch(cond, runcardRawInfo, rule), recipeId);
        if (r != null) return r;

        WaferCondition wc = dataLoaderService.getWaferCondition(runcardRawInfo.getRuncardId());
        if (wc == null) {
            log.info("RuncardID: {} Condition: {} - No WaferCondition data => skip",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = new HashMap<>();
            detail.put("error", "No WaferCondition data => skip");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }
        log.info("RuncardID: {} Condition: {} - WaferCondition retrieved data: {}",
                runcardRawInfo.getRuncardId(), cond, wc);

        int uniqueCount = RuleUtil.parseIntSafe(wc.getUniqueCount());
        int wfrQty = RuleUtil.parseIntSafe(wc.getWfrQty());

        boolean isEqual = (uniqueCount == wfrQty);
        int lamp = isEqual ? 1 : 3;

        log.info("RuncardID: {} Condition: {} - WaferCondition check => uniqueCount = '{}', wfrQty = '{}'",
                runcardRawInfo.getRuncardId(), cond, uniqueCount, wfrQty);

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeId);
        detailMap.put("result", lamp);
        detailMap.put("waferCondition", isEqual);
        detailMap.put("wfrQty", wfrQty);
        detailMap.put("experimentQty", uniqueCount);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - WaferCondition detail = {}",
                runcardRawInfo.getRuncardId(), cond, detailMap);

        log.info("RuncardID: {} Condition: {} - WaferCondition check done, lamp = '{}'",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }
}
