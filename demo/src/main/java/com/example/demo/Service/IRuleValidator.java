package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;

import java.util.List;

public interface IRuleValidator {
    void preLayerProcess(Runcard runcard);
    List<String> validateRule(Runcard runcard, List<Rule> rules);
    String parseResult(List<String> results);
}

