package com.example.demo.rule;

import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleInhibitionCheckStatus implements IRuleCheck {

    private final DataLoaderService dataLoaderService;

    @Override
    public ResultInfo check(RuncardRawInfo runcardRawInfo, Rule rule) {
        ResultInfo info = new ResultInfo();
        info.setRuleType(rule.getRuleType());

        if (RuleUtil.isLotTypeEmpty(rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType is empty => skip check"));
            return info;
        }

        if (RuleUtil.shouldCheckLotType(runcardRawInfo, rule)) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "lotType mismatch => skip check"));
            return info;
        }

        List<InhibitionCheckStatus> list = dataLoaderService.getInhibitionCheckStatus();
        if (list.isEmpty()) {
            info.setResult(0);
            info.setDetail(Collections.singletonMap("msg", "No InhibitionCheckStatus => skip"));
            return info;
        }

        boolean allY = list.stream().allMatch(ics -> "Y".equalsIgnoreCase(ics.getInhibitFlag()));
        int lamp = allY ? 1 : 2;

        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "inhibitionCheck", allY
        );

        info.setResult(lamp);
        info.setDetail(detailMap);
        return info;
    }
}
