package com.example.demo.rule;

import com.example.demo.vo.ResultInfo;
import com.example.demo.vo.Rule;
import com.example.demo.vo.RuncardRawInfo;
import org.springframework.stereotype.Component;

@Component
public interface IRuleCheck {
    /**
     * @return 回傳包含燈號 & 詳細訊息的檢查結果
     * TODO detail 裡需要包含 <result ,(1 or 2 or 3)>
     * TODO 每個 check 都需要先做 lot type filter
     */
    ResultInfo check(String cond, RuncardRawInfo runcardRawInfoId, Rule rule);
}


