package com.example.demo.rule;

import com.example.demo.po.WaferCondition;
import com.example.demo.service.DataLoaderService;
import com.example.demo.utils.RuleUtil;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RuleWaferCondition implements IRuleCheck {

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

        WaferCondition wc = dataLoaderService.getWaferCondition();
        int uniqueCount = parseIntSafe(wc.getUniqueCount());
        int wfrQty = parseIntSafe(wc.getWfrQty());

        boolean isEqual = (uniqueCount == wfrQty);
        int lamp = isEqual ? 1 : 3;

        info.setResult(lamp);
        Map<String, Object> detailMap = Map.of(
                "result", lamp,
                "waferCondition", isEqual,
                "wfrQty", wfrQty,
                "experimentQty", uniqueCount
        );
        info.setDetail(detailMap);

        return info;
    }

    private int parseIntSafe(String s) {
        if (s == null) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
