package com.example.demo.rule;

import com.example.demo.vo.RecipeGroupsAndToolInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class RuncardDao {

    /**
     * 模擬從某資料庫 / Mongo 取得 RecipeGroupsAndToolInfo
     */
    public Optional<List<RecipeGroupsAndToolInfo>> getRecipeGroupsAndToolInfos() {
        // 造一些假資料
        List<RecipeGroupsAndToolInfo> mockList = new ArrayList<>();

        RecipeGroupsAndToolInfo info1 = new RecipeGroupsAndToolInfo(
                "cond1",
                "RG-001",
                "JDTM16,JDTM17,JDTM20",
                "xxx.xx-xxxx.xxxx-{cEF}{c134}"
        );
        RecipeGroupsAndToolInfo info2 = new RecipeGroupsAndToolInfo(
                "cond2",
                "RG-002",
                "ToolA,ToolB",
                "yyy.yy-yyyy.yyyy-{c(2;3)}"
        );

        mockList.add(info1);
        mockList.add(info2);

        // 回傳 Optional
        return Optional.of(mockList);
    }
}
