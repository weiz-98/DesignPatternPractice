package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface IRuleValidator {
    List<ResultInfo> validateRule(RuncardRawInfo runcardRawInfo, List<Rule> rules);

    List<ResultInfo> parseResult(List<ResultInfo> resultInfos);
}
