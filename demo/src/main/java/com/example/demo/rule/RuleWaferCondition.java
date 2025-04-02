package com.example.demo.rule;

import com.example.demo.po.WaferCondition;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleWaferCondition implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - Start RuleWaferCondition check",
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

        WaferCondition wc = dataLoaderService.getWaferCondition();
        if (wc == null) {
            log.info("RuncardID: {} Condition: {} - No WaferCondition data => skip",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = Map.of(
                    "error", "No WaferCondition data => skip",
                    "runcardId", runcardRawInfo.getRuncardId(),
                    "condition", cond
            );
            info.setDetail(detail);
            return info;
        }

        int uniqueCount = RuleUtil.parseIntSafe(wc.getUniqueCount());
        int wfrQty = RuleUtil.parseIntSafe(wc.getWfrQty());

        boolean isEqual = (uniqueCount == wfrQty);
        int lamp = isEqual ? 1 : 3;

        log.info("RuncardID: {} Condition: {} - WaferCondition => uniqueCount={}, wfrQty={}, finalLamp={}",
                runcardRawInfo.getRuncardId(), cond, uniqueCount, wfrQty, lamp);

        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "waferCondition", isEqual,
                "wfrQty", wfrQty,
                "experimentQty", uniqueCount,
                "runcardId", runcardRawInfo.getRuncardId(),
                "condition", cond
        );
        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RuleWaferCondition done, lamp={}",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }
}
