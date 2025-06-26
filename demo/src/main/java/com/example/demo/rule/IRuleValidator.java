package com.example.demo.rule;

import com.example.demo.service.BatchCache;
import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface IRuleValidator {
    List<ResultInfo> validateRule(String cond, RuncardRawInfo runcardRawInfo, List<Rule> rules, BatchCache cache);

    List<ResultInfo> parseResult(List<ResultInfo> resultInfos);
}
