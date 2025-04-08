package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeGroupAndToolInfo {
    private String condition;
    private String recipeGroupId;
    private String toolIdList;  // 可能是 "JDTM16,JDTM17,JDTM20"
    private String recipeId;    // 例如 "xxx.xx-xxxx.xxxx-{cEF}{c134}"
}
