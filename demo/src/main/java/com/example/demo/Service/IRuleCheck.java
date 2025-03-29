package com.example.demo.Service;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

@Component
public interface IRuleCheck {
    /**
     * @param runcardRawInfoId  目前這個 runcard 的 id
     * @param rule       要檢查的規則
     * @return           回傳包含燈號 & 詳細訊息的檢查結果
     */
    ResultInfo check(RuncardRawInfo runcardRawInfoId, Rule rule);
}


