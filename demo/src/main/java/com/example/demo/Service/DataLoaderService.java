package com.example.demo.Service;

import com.example.demo.po.ForwardProcess;
import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.po.WaferCondition;
import com.example.demo.vo.ModuleInfo;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.RuncardRawInfo;
import com.example.demo.vo.ToolRuleGroup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataLoaderService {

    private final RuleDao ruleDao;

    // 取得 ForwardProcess 資料
    public List<ForwardProcess> getForwardProcess() {
        Optional<List<ForwardProcess>> opt = ruleDao.getForwardProcess();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 InhibitionCheckStatus 資料
    public List<InhibitionCheckStatus> getInhibitionCheckStatus() {
        Optional<List<InhibitionCheckStatus>> opt = ruleDao.getInhibitionCheckStatus();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 RecipeGroupCheckBlue 資料
    public List<RecipeGroupCheckBlue> getRecipeGroupCheckBlue() {
        Optional<List<RecipeGroupCheckBlue>> opt = ruleDao.getRecipeGroupCheckBlue();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 WaferCondition 資料
    public List<WaferCondition> getWaferCondition() {
        Optional<WaferCondition> opt = ruleDao.getWaferCondition();
        return opt.map(Collections::singletonList).orElse(Collections.emptyList());
    }

    public List<ModuleInfo> getModules() {
        // 原本取得的模組清單
        List<String> modules = Arrays.asList("ModuleA", "ModuleB", "ModuleC");

        // 將 String 型態的模組名稱轉換成 ModuleInfo 物件
        List<ModuleInfo> moduleInfos = modules.stream()
                // 假設先以空的 sectionIds 傳回
                .map(moduleName -> new ModuleInfo(moduleName, Collections.emptyList()))
                .collect(Collectors.toList());

        log.info("[getModules] moduleInfos = {}", moduleInfos);
        return moduleInfos;
    }

    /**
     * 2. 根據 module List 去 MongoDB 尋找對應的 ToolGroup 資訊
     * 回傳 Optional<List<ToolGroup>>
     */
    public List<ToolRuleGroup> getToolRuleGroups(List<String> sectionIds) {
        // 這裡只是 Mock：實務上可根據 moduleList 去 Mongo DB 查詢
        if (sectionIds == null || sectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Mock 產生 ToolGroup 清單
        List<ToolRuleGroup> mockToolGroups = new ArrayList<>();

        // Demo: 如果包含 ModuleA，就先造一個 ToolGroup
        if (sectionIds.contains("ModuleA")) {
            ToolRuleGroup groupA = new ToolRuleGroup();
            groupA.setGroupName("GroupA");
            groupA.setModule("ModuleA");
            // ... 其他屬性可自行填寫
            // groupA.setTools(...);
            // groupA.setRules(...);
            mockToolGroups.add(groupA);
        }

        // Demo: 如果包含 ModuleB，就再造一個 ToolGroup
        if (sectionIds.contains("ModuleB")) {
            ToolRuleGroup groupB = new ToolRuleGroup();
            groupB.setGroupName("GroupB");
            groupB.setModule("ModuleB");
            // ... 其他屬性可自行填寫
            mockToolGroups.add(groupB);
        }

        // 也可以視需要再加更多 group
        return mockToolGroups;
    }


    /**
     * 3. 根據 module List 去尋找指定時間區間內的 Runcard
     * 回傳 List<Runcard>
     */
    public List<RuncardRawInfo> getMockRUncardRawInfoList(List<String> moduleList, LocalDateTime startTime, LocalDateTime endTime) {
        // 模擬在某 DB (Mongo / SQL / 其他) 查 Runcard
        // 範例僅用固定資料
        List<RuncardRawInfo> mockRuncardRawInfos = new ArrayList<>();

        // 如果包含 ModuleA，造一些 Runcard
        if (moduleList.contains("ModuleA")) {
            RuncardRawInfo rc1 = new RuncardRawInfo("RC-001", "PART-123", "OPE-10", "2025-03-10 08:00:00",
                    "2025-03-10 06:00:00", "ManagerA", "EngineerA");
            RuncardRawInfo rc2 = new RuncardRawInfo("RC-002", "PART-456", "OPE-20", "2025-03-11 10:00:00",
                    "2025-03-11 09:50:00", "ManagerB", "EngineerB");
            mockRuncardRawInfos.add(rc1);
            mockRuncardRawInfos.add(rc2);
        }

        // 如果包含 ModuleB，也造一些
        if (moduleList.contains("ModuleB")) {
            RuncardRawInfo rc3 = new RuncardRawInfo("RC-003", "PART-999", "OPE-30", "2025-03-12 15:00:00",
                    "2025-03-12 14:59:00", "ManagerC", "EngineerC");
            mockRuncardRawInfos.add(rc3);
        }

        // 篩選出符合日期區間的
        // (實務上會在查詢時就把日期條件放進去，但這裡僅作 Mock)
        List<RuncardRawInfo> filtered = new ArrayList<>();
        for (RuncardRawInfo rc : mockRuncardRawInfos) {
            // 假設 arriveAt 為 "yyyy-MM-dd HH:mm:ss"，需先轉成 LocalDateTime 來做比較
            LocalDateTime arriveTime = parseLocalDateTime(rc.getArriveAt());
            if (arriveTime != null && !arriveTime.isBefore(startTime) && !arriveTime.isAfter(endTime)) {
                filtered.add(rc);
            }
        }

        log.info("[findRuncardByModule] moduleList={}, start={}, end={}, resultCount={}",
                moduleList, startTime, endTime, filtered.size());
        return filtered;
    }

    /**
     * 4. 根據 runcardId 取得對應的 RecipeAndToolInfo
     * 回傳 Optional<List<RecipeAndToolInfo>>
     */
    public List<OneConditionRecipeAndToolInfo> getRecipeAndToolInfo(String runcardId) {
        // 這裡也只做 Mock
        if (runcardId == null || runcardId.isEmpty()) {
            return Collections.emptyList();
        }

        List<OneConditionRecipeAndToolInfo> mockList = new ArrayList<>();
        // Demo: 固定填一些示例
        OneConditionRecipeAndToolInfo info1 = new OneConditionRecipeAndToolInfo("condition1", "JDTM16,JDTM17,JDTM20", "xxx.xx-xxxx.xxxx-{cEF}{c134}");
        OneConditionRecipeAndToolInfo info2 = new OneConditionRecipeAndToolInfo("condition2", "RG-2026", "yyy.yy-yyyy.yyyy-{c{2;3}}");
        mockList.add(info1);
        mockList.add(info2);

        // 也可根據 runcardId 來做篩選，這裡單純回傳兩筆測試資料
        return mockList;
    }


    /**
     * 小工具: 將字串 "yyyy-MM-dd HH:mm:ss" 轉成 LocalDateTime
     */
    private LocalDateTime parseLocalDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr.replace(" ", "T"));
        } catch (Exception e) {
            log.warn("parseLocalDateTime() error, input={}", dateTimeStr);
            return null;
        }
    }
}


