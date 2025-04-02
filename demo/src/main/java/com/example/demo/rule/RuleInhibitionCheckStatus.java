package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RuleInhibitionCheckStatus implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(String cond, RuncardRawInfo runcardRawInfo, Rule rule) {
        log.info("RuncardID: {} Condition: {} - Start RuleInhibitionCheckStatus check",
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

        List<InhibitionCheckStatus> list = dataLoaderService.getInhibitionCheckStatus();
        if (list.isEmpty()) {
            log.info("RuncardID: {} Condition: {} - No InhibitionCheckStatus data => skip",
                    runcardRawInfo.getRuncardId(), cond);

            info.setResult(3);
            Map<String, Object> detail = Map.of(
                    "error", "No InhibitionCheckStatus data => skip",
                    "runcardId", runcardRawInfo.getRuncardId(),
                    "condition", cond
            );
            info.setDetail(detail);
            return info;
        }

        boolean allY = list.stream().allMatch(ics -> "Y".equalsIgnoreCase(ics.getInhibitFlag()));
        int lamp = allY ? 1 : 2;

        log.info("RuncardID: {} Condition: {} - InhibitionCheckStatus => allY={}, finalLamp={}",
                runcardRawInfo.getRuncardId(), cond, allY, lamp);

        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "inhibitionCheck", allY,
                "runcardId", runcardRawInfo.getRuncardId(),
                "condition", cond
        );

        info.setResult(lamp);
        info.setDetail(detailMap);

        log.info("RuncardID: {} Condition: {} - RuleInhibitionCheckStatus done, lamp={}",
                runcardRawInfo.getRuncardId(), cond, lamp);

        return info;
    }
}
