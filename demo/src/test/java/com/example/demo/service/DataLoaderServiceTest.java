package com.example.demo.service;

import com.example.demo.rule.RuleDao;
import com.example.demo.rule.RuncardInfoDao;
import com.example.demo.vo.MultipleRecipeData;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.RecipeGroupAndTool;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Slf4j
@ExtendWith(MockitoExtension.class)
class DataLoaderServiceTest {

    @Mock
    private RuleDao ruleDao;

    @Mock
    private RuncardInfoDao runcardInfoDao;

    @InjectMocks
    private DataLoaderService dataLoaderService;

    /**
     * case 1: RecipeGroups 和 MultipleRecipeData 都沒有資料 => 結果為空清單
     */
    @Test
    void testGetRecipeAndToolInfo_noData() {
        // Mock: 都回傳 Optional.empty()
        when(runcardInfoDao.getRecipeGroupsAndToolInfos())
                .thenReturn(Optional.empty());
        when(runcardInfoDao.multipleRecipeData())
                .thenReturn(Optional.empty());

        // 執行
        List<OneConditionRecipeAndToolInfo> result = dataLoaderService.getRecipeAndToolInfo("RC-001");
        log.info("result: {}", result);

        // 驗證結果
        assertNotNull(result);
        assertTrue(result.isEmpty(), "預期回傳空清單");

        // 驗證呼叫次數
        verify(runcardInfoDao, times(1)).getRecipeGroupsAndToolInfos();
        verify(runcardInfoDao, times(1)).multipleRecipeData();
    }

    /**
     * case 2: 只有 RecipeGroupAndTool 有資料，MultipleRecipeData 沒資料
     */
    @Test
    void testGetRecipeAndToolInfo_onlyRecipeGroup() {
        // 準備 mock 資料
        RecipeGroupAndTool rgt = RecipeGroupAndTool.builder()
                .condition("COND-XYZ")
                .recipeGroupId("G-001")
                .toolIdList("TOOL1,TOOL2")
                .recipeId("recipeAB")
                .build();

        // Mock：RecipeGroupAndTool => 1 筆，MultipleRecipeData => empty
        when(runcardInfoDao.getRecipeGroupsAndToolInfos())
                .thenReturn(Optional.of(Collections.singletonList(rgt)));
        when(runcardInfoDao.multipleRecipeData())
                .thenReturn(Optional.empty());

        // 執行
        List<OneConditionRecipeAndToolInfo> result = dataLoaderService.getRecipeAndToolInfo("RC-001");
        log.info("result: {}", result);
        // 驗證結果
        assertNotNull(result);
        assertEquals(1, result.size(), "應該只有一筆資料");
        OneConditionRecipeAndToolInfo info = result.get(0);
        assertEquals("COND-XYZ", info.getCondition());
        assertEquals("TOOL1,TOOL2", info.getToolIdList());
        assertEquals("recipeAB", info.getRecipeId());

        // 驗證呼叫次數
        verify(runcardInfoDao, times(1)).getRecipeGroupsAndToolInfos();
        verify(runcardInfoDao, times(1)).multipleRecipeData();
    }

    /**
     * case 3: 只有 MultipleRecipeData，有多筆資料，RecipeGroupAndTool 沒資料
     */
    @Test
    void testGetRecipeAndToolInfo_onlyMultipleRecipeData() {
        // Mock：getRecipeGroupsAndToolInfos => empty
        when(runcardInfoDao.getRecipeGroupsAndToolInfos())
                .thenReturn(Optional.empty());

        // 準備 MultipleRecipeData：condition=01 有兩組 suffix=01,02；condition=02 有一組 suffix=01
        MultipleRecipeData d1 = MultipleRecipeData.builder()
                .condition("01").name("RC_RECIPE_ID_01").value("Recipe-01").build();      // => RECIPE
        MultipleRecipeData d2 = MultipleRecipeData.builder()
                .condition("01").name("RC_RECIPE_ID_01_EQP_OA").value("Tool-ABC").build(); // => TOOL
        MultipleRecipeData d3 = MultipleRecipeData.builder()
                .condition("01").name("RC_RECIPE_ID_02").value("Recipe-02").build();
        MultipleRecipeData d4 = MultipleRecipeData.builder()
                .condition("01").name("RC_RECIPE_ID_02_EQP_OA").value("Tool-XYZ").build();

        // condition=02 => suffix=01 只有 RECIPE
        MultipleRecipeData d5 = MultipleRecipeData.builder()
                .condition("02").name("RC_RECIPE_ID_01").value("Recipe-BB").build();

        List<MultipleRecipeData> mockData = Arrays.asList(d1, d2, d3, d4, d5);

        // Mock multipleRecipeData => 以上集合
        when(runcardInfoDao.multipleRecipeData())
                .thenReturn(Optional.of(mockData));

        // 執行
        List<OneConditionRecipeAndToolInfo> result = dataLoaderService.getRecipeAndToolInfo("RC-999");
        log.info("result: {}", result);
        // 驗證結果 => 3 筆
        assertNotNull(result);
        assertEquals(3, result.size(), "預期 3 筆");

        // 轉成 Map 方便比對
        Map<String, OneConditionRecipeAndToolInfo> mapByCond =
                result.stream().collect(Collectors.toMap(OneConditionRecipeAndToolInfo::getCondition, x -> x));

        // "01_M01" => recipeId="Recipe-01", toolIdList="Tool-ABC"
        OneConditionRecipeAndToolInfo item1 = mapByCond.get("01_M01");
        assertNotNull(item1);
        assertEquals("Recipe-01", item1.getRecipeId());
        assertEquals("Tool-ABC", item1.getToolIdList());

        // "01_M02" => recipeId="Recipe-02", toolIdList="Tool-XYZ"
        OneConditionRecipeAndToolInfo item2 = mapByCond.get("01_M02");
        assertNotNull(item2);
        assertEquals("Recipe-02", item2.getRecipeId());
        assertEquals("Tool-XYZ", item2.getToolIdList());

        // "02_M01" => recipeId="Recipe-BB", toolIdList=""
        OneConditionRecipeAndToolInfo item3 = mapByCond.get("02_M01");
        assertNotNull(item3);
        assertEquals("Recipe-BB", item3.getRecipeId());
        assertEquals("", item3.getToolIdList(), "應該沒有對應的 TOOL => 空字串");

        // 驗證 dao 被呼叫
        verify(runcardInfoDao, times(1)).getRecipeGroupsAndToolInfos();
        verify(runcardInfoDao, times(1)).multipleRecipeData();
    }

    /**
     * case 4: 同時有 RecipeGroupAndTool 與 MultipleRecipeData
     */
    @Test
    void testGetRecipeAndToolInfo_mixedData() {
        // 1) Mock RecipeGroupAndTool
        RecipeGroupAndTool rgt1 = RecipeGroupAndTool.builder()
                .condition("COND-XYZ")
                .recipeGroupId("G-001")
                .toolIdList("TOOL-11,TOOL-22")
                .recipeId("recipe-XYZ")
                .build();

        RecipeGroupAndTool rgt2 = RecipeGroupAndTool.builder()
                .condition("COND-ABC")
                .recipeGroupId("G-002")
                .toolIdList("TOOL-99")
                .recipeId("recipe-ABC")
                .build();

        when(runcardInfoDao.getRecipeGroupsAndToolInfos())
                .thenReturn(Optional.of(List.of(rgt1, rgt2)));

        // 2) Mock MultipleRecipeData
        // condition=01 => suffix=01,02
        MultipleRecipeData d1 = new MultipleRecipeData("01", "RC_RECIPE_ID_01", "mRecipe-01");
        MultipleRecipeData d2 = new MultipleRecipeData("01", "RC_RECIPE_ID_01_EQP_OA", "mTool-ABC");
        MultipleRecipeData d3 = new MultipleRecipeData("01", "RC_RECIPE_ID_02", "mRecipe-02");
        MultipleRecipeData d4 = new MultipleRecipeData("01", "RC_RECIPE_ID_02_EQP_OA", "mTool-XYZ");

        // condition=02 => suffix=01
        MultipleRecipeData d5 = new MultipleRecipeData("02", "RC_RECIPE_ID_01", "mRecipe-BB");

        when(runcardInfoDao.multipleRecipeData())
                .thenReturn(Optional.of(List.of(d1, d2, d3, d4, d5)));

        // 執行
        List<OneConditionRecipeAndToolInfo> result = dataLoaderService.getRecipeAndToolInfo("RC-111");
        log.info("result: {}", result);
        // 總共應該 5 筆：2(RecipeGroup) + 3(MultipleRecipeData)
        assertNotNull(result);
        assertEquals(5, result.size(), "總共有 5 筆資料");

        // 轉成 Map<condition, info> 做檢查
        Map<String, OneConditionRecipeAndToolInfo> mapByCond =
                result.stream().collect(Collectors.toMap(OneConditionRecipeAndToolInfo::getCondition, x -> x, (a, b) -> b));

        // 檢查 RecipeGroupAndTool -> 2 筆
        OneConditionRecipeAndToolInfo rgtItem1 = mapByCond.get("COND-XYZ");
        assertNotNull(rgtItem1);
        assertEquals("TOOL-11,TOOL-22", rgtItem1.getToolIdList());
        assertEquals("recipe-XYZ", rgtItem1.getRecipeId());

        OneConditionRecipeAndToolInfo rgtItem2 = mapByCond.get("COND-ABC");
        assertNotNull(rgtItem2);
        assertEquals("TOOL-99", rgtItem2.getToolIdList());
        assertEquals("recipe-ABC", rgtItem2.getRecipeId());

        // 檢查 MultipleRecipeData -> 3 筆
        OneConditionRecipeAndToolInfo m1 = mapByCond.get("01_M01");
        assertNotNull(m1);
        assertEquals("mRecipe-01", m1.getRecipeId());
        assertEquals("mTool-ABC", m1.getToolIdList());

        OneConditionRecipeAndToolInfo m2 = mapByCond.get("01_M02");
        assertNotNull(m2);
        assertEquals("mRecipe-02", m2.getRecipeId());
        assertEquals("mTool-XYZ", m2.getToolIdList());

        OneConditionRecipeAndToolInfo m3 = mapByCond.get("02_M01");
        assertNotNull(m3);
        assertEquals("mRecipe-BB", m3.getRecipeId());
        assertEquals("", m3.getToolIdList());

        // 驗證 dao 呼叫次數
        verify(runcardInfoDao, times(1)).getRecipeGroupsAndToolInfos();
        verify(runcardInfoDao, times(1)).multipleRecipeData();
    }

}
