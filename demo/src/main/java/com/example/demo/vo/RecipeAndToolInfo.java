package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeAndToolInfo {
    private String conditions; // 除了代表 runcard 的 condition 外，也可以代表 multiple recipe
    private String recipeGroupId;
    private String toolIdList;  // 可能是 "JDTM16,JDTM17,JDTM20"
    private String recipeId;    // 例如 "xxx.xx-xxxx.xxxx-{cEF}{c134}"
}


