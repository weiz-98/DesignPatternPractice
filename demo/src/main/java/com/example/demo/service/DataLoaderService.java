package com.example.demo.service;

import com.example.demo.po.ForwardProcess;
import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.po.WaferCondition;
import com.example.demo.rule.RuleDao;
import com.example.demo.rule.RuncardInfoDao;
import com.example.demo.vo.*;
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
    private final RuncardInfoDao runcardInfoDao;

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
    public List<RecipeGroupCheckBlue> getRecipeGroupCheckBlue(String recipeGroupId, List<String> toolIds) {
        Optional<List<RecipeGroupCheckBlue>> opt = ruleDao.getRecipeGroupCheckBlue();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 WaferCondition 資料 (現在 RuleDao 回傳 Optional<WaferCondition>)
    public WaferCondition getWaferCondition() {
        Optional<WaferCondition> opt = ruleDao.getWaferCondition();
        return opt.orElseGet(WaferCondition::new);
    }


    /**
     * 取得所有模組 (原本就存在)
     */
    public List<ModuleInfo> getModules() {
        List<String> modules = Arrays.asList("ModuleA", "ModuleB", "ModuleC");
        List<ModuleInfo> moduleInfos = modules.stream()
                .map(moduleName -> new ModuleInfo(moduleName, Collections.emptyList()))
                .collect(Collectors.toList());
        log.info("[getModules] moduleInfos = {}", moduleInfos);
        return moduleInfos;
    }

    /**
     * 2. 根據 sectionIds 去 MongoDB 尋找對應的 ToolGroup 資訊 (Mock)
     */
    public List<ToolRuleGroup> getToolRuleGroups(List<String> sectionIds) {
        if (sectionIds == null || sectionIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<ToolRuleGroup> mockToolGroups = new ArrayList<>();

        if (sectionIds.contains("ModuleA")) {
            ToolRuleGroup groupA = new ToolRuleGroup();
            groupA.setGroupName("GroupA");
            groupA.setModule("ModuleA");
            // groupA.setTools(...);
            // groupA.setRules(...);
            mockToolGroups.add(groupA);
        }
        if (sectionIds.contains("ModuleB")) {
            ToolRuleGroup groupB = new ToolRuleGroup();
            groupB.setGroupName("GroupB");
            groupB.setModule("ModuleB");
            mockToolGroups.add(groupB);
        }

        return mockToolGroups;
    }

    /**
     * 3. 根據 moduleList 取得 RuncardRawInfo
     * 這裡移除對「時間範圍」的檢查，因新版 RuncardRawInfo 沒有日期欄位
     */
    public List<RuncardRawInfo> getMockRUncardRawInfoList(List<String> moduleList, LocalDateTime startTime, LocalDateTime endTime) {
        // Mock 生成一些 RuncardRawInfo
        List<RuncardRawInfo> mockRuncardRawInfos = new ArrayList<>();

        // 如果包含 ModuleA
        if (moduleList.contains("ModuleA")) {
            // 新版 RuncardRawInfo:
            // (String runcardId, String issuingEngineer, String lotId, String partId,
            //  String status, String purpose, String supervisorAndDepartment, Integer numberOfPieces, String holdAtOperNo)
            RuncardRawInfo rc1 = new RuncardRawInfo(
                    "RC-001",              // runcardId
                    "EngineerA",           // issuingEngineer
                    "LOT-123",             // lotId
                    "PART-ABC",            // partId
                    "ACTIVE",              // status
                    "TestPurpose",         // purpose
                    "SupervisorA-DeptA",   // supervisorAndDepartment
                    100,                   // numberOfPieces
                    "OPE-10"               // holdAtOperNo
            );
            RuncardRawInfo rc2 = new RuncardRawInfo(
                    "RC-002",
                    "EngineerB",
                    "LOT-456",
                    "PART-XYZ",
                    "ACTIVE",
                    "DebugPurpose",
                    "SupervisorB-DeptB",
                    50,
                    "OPE-20"
            );
            mockRuncardRawInfos.add(rc1);
            mockRuncardRawInfos.add(rc2);
        }

        // 如果包含 ModuleB
        if (moduleList.contains("ModuleB")) {
            RuncardRawInfo rc3 = new RuncardRawInfo(
                    "RC-003",
                    "EngineerC",
                    "LOT-999",
                    "PART-999",
                    "COMPLETE",
                    "Production",
                    "SupervisorC-DeptC",
                    200,
                    "OPE-30"
            );
            mockRuncardRawInfos.add(rc3);
        }

        // 這裡原先可能要根據 startTime / endTime 做日期篩選，但現在無日期欄位可對照
        // 因此只印 log
        log.info("[getMockRUncardRawInfoList] moduleList={}, start={}, end={}, totalCount={}",
                moduleList, startTime, endTime, mockRuncardRawInfos.size());
        return mockRuncardRawInfos;
    }

    /**
     * 4. 根據 runcardId 取得對應的 OneConditionRecipeAndToolInfo
     */
    public List<OneConditionRecipeAndToolInfo> getRecipeAndToolInfo(String runcardId) {
        if (runcardId == null || runcardId.isEmpty()) {
            return Collections.emptyList();
        }
        // 依照 runcardId mock 出一些資料
        List<OneConditionRecipeAndToolInfo> mockList = new ArrayList<>();

        OneConditionRecipeAndToolInfo info1 = new OneConditionRecipeAndToolInfo(
                "condition1",
                "JDTM16,JDTM17,JDTM20",
                "xxx.xx-xxxx.xxxx-{cEF}{c134}"
        );
        OneConditionRecipeAndToolInfo info2 = new OneConditionRecipeAndToolInfo(
                "condition2",
                "RG-2026",
                "yyy.yy-yyyy.yyyy-{c(2;3)}"
        );
        mockList.add(info1);
        mockList.add(info2);

        // 視 runcardId 亦可做區分，如若為 "RC-002" 再加別的
        if ("RC-002".equals(runcardId)) {
            OneConditionRecipeAndToolInfo info3 = new OneConditionRecipeAndToolInfo(
                    "condition2",
                    "ToolX,ToolY",
                    "zzz.zz-zzzz.zzzz-{c}"
            );
            mockList.add(info3);
        }

        log.info("[getRecipeAndToolInfo] runcardId={}, returnSize={}", runcardId, mockList.size());
        return mockList;
    }

    public List<RecipeGroupAndTool> getRecipeGroupAndToolInfo() {
        Optional<List<RecipeGroupAndTool>> opt = runcardInfoDao.getRecipeGroupsAndToolInfos();
        return opt.orElseGet(ArrayList::new);
    }

    public List<MultipleRecipeData> getMultipleRecipeData() {
        Optional<List<MultipleRecipeData>> opt = runcardInfoDao.multipleRecipeData();
        return opt.orElseGet(ArrayList::new);
    }

}
