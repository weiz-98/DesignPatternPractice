package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResultInfo {
    private String ruleType;                // 規則類型 (例如 ruleA, ruleB, ... 以及 no-group )
    private Map<String, Object> detail;     // 細部資訊 (顯示判斷邏輯、錯誤原因或其他補充資訊)
    private Integer result;                 // 燈號: 1=綠燈, 2=黃燈, 3=紅燈, 0=沒有mapping到
}

