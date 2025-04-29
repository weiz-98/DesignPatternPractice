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

        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        ResultInfo r;
        r = RuleUtil.checkLotTypeEmpty(cond, runcardRawInfo, rule);
        if (r != null) return r;
        r = RuleUtil.checkLotTypeMismatch(cond, runcardRawInfo, rule);
        if (r != null) return r;

        List<InhibitionCheckStatus> list = dataLoaderService.getInhibitionCheckStatus(runcardRawInfo.getRuncardId());
        if (list.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No InhibitionCheckStatus data => skip",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = new HashMap<>();
            detail.put("error", "No InhibitionCheckStatus data => skip");
            detail.put("runcardId", runcardRawInfo.getRuncardId());
            detail.put("condition", cond);
            detail.put("lotType", rule.getLotType());

            info.setDetail(detail);
            return info;
        }

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus retrieved {} rows",
                runcardRawInfo.getRuncardId(), cond,
                list.size());

        boolean allY = list.stream().allMatch(ics -> "Y".equalsIgnoreCase(ics.getInhibitFlag()));
        int lamp = allY ? 1 : 2;

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check => allY={}",
                runcardRawInfo.getRuncardId(), cond, allY);

        String recipeId = dataLoaderService.getRecipeAndToolInfo(runcardRawInfo.getRuncardId())
                .stream()
                .filter(o -> cond.equals(o.getCondition()))
                .map(OneConditionRecipeAndToolInfo::getRecipeId)
                .findFirst()
                .orElse("");

        Map<String, Object> detailMap = new HashMap<>();
        detailMap.put("recipeId", recipeId);
        detailMap.put("result", lamp);
        detailMap.put("inhibitionCheck", allY);
        detailMap.put("runcardId", runcardRawInfo.getRuncardId());
        detailMap.put("condition", cond);
        detailMap.put("lotType", rule.getLotType());

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus check done, lamp={}",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }
}
