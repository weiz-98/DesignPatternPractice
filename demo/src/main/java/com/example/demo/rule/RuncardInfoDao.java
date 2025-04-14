package com.example.demo.rule;

import com.example.demo.vo.MultipleRecipeData;
import com.example.demo.vo.RecipeGroupAndTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class RuncardInfoDao {

    /**
     * 模擬從某資料庫 / Mongo 取得 RecipeGroupsAndToolInfo
     */
    public Optional<List<RecipeGroupAndTool>> getRecipeGroupsAndToolInfos() {
        // 造一些假資料
        List<RecipeGroupAndTool> mockList = new ArrayList<>();

        RecipeGroupAndTool info1 = new RecipeGroupAndTool(
                "cond1",
                "RG-001",
                "JDTM16,JDTM17,JDTM20",
                "xxx.xx-xxxx.xxxx-{cEF}{c134}"
        );
        RecipeGroupAndTool info2 = new RecipeGroupAndTool(
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

    /**
     * 模擬從某資料庫 / Mongo 取得 MultipleRecipeData
     */
    public Optional<List<MultipleRecipeData>> multipleRecipeData() {
        List<MultipleRecipeData> mockMultipleRecipeDataList = new ArrayList<>();

        // 假資料 1
        MultipleRecipeData data1 = MultipleRecipeData.builder()
                .condition("condition1")
                .name("recipeName1")
                .value("recipeValue1")
                .build();

        // 假資料 2
        MultipleRecipeData data2 = MultipleRecipeData.builder()
                .condition("condition2")
                .name("recipeName2")
                .value("recipeValue2")
                .build();

        mockMultipleRecipeDataList.add(data1);
        mockMultipleRecipeDataList.add(data2);

        // 回傳 Optional
        return Optional.of(mockMultipleRecipeDataList);
    }
}
