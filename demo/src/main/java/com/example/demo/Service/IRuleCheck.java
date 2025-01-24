package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;

public interface IRuleCheck {
    String execute(Runcard runcard, Rule rule);
}

