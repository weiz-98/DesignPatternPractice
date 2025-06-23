package com.example.demo.service;

import com.example.demo.po.*;
import com.example.demo.rule.RCOwnerDao;
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
    private final RCOwnerDao rcOwnerDao;

    public Map<String, String> getToolIdToSectNameMap() {
        return Map.of("s", "s");
    }

    // 取得 ForwardProcess 資料
    public List<ForwardProcess> getForwardProcess(String runcardId) {
        Optional<List<ForwardProcess>> opt = ruleDao.getForwardProcess();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 InhibitionCheckStatus 資料
    public List<InhibitionCheckStatus> getInhibitionCheckStatus(String runcardId) {
        Optional<List<InhibitionCheckStatus>> opt = ruleDao.getInhibitionCheckStatus();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 RecipeGroupCheckBlue 資料
    public List<RecipeGroupCheckBlue> getRecipeGroupCheckBlue(String recipeGroupId, List<String> toolIds) {
        Optional<List<RecipeGroupCheckBlue>> opt = ruleDao.getRecipeGroupCheckBlue();
        return opt.orElseGet(ArrayList::new);
    }

    // 取得 WaferCondition 資料 (現在 RuleDao 回傳 Optional<WaferCondition>)
    public WaferCondition getWaferCondition(String runcardId) {
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
    public Optional<List<RuncardRawInfo>> getQueryRuncardBatch(List<String> sectionIds, LocalDateTime startTime, LocalDateTime endTime) {
        // Mock 生成一些 RuncardRawInfo
        List<RuncardRawInfo> mockRuncardRawInfos = new ArrayList<>();

        // 如果包含 ModuleA
        if (sectionIds.contains("ModuleA")) {
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
        if (sectionIds.contains("ModuleB")) {
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
                sectionIds, startTime, endTime, mockRuncardRawInfos.size());
        return Optional.of(mockRuncardRawInfos);
    }

    public List<OneConditionRecipeAndToolInfo> getRecipeAndToolInfo(String runcardId) {
        // 1) 先拿到兩份資料
        List<RecipeGroupAndTool> recipeGroupAndToolList = getRecipeGroupAndTool(runcardId);
        List<MultipleRecipeData> multipleRecipeDataList = getMultipleRecipeData(runcardId);

        List<OneConditionRecipeAndToolInfo> result = new ArrayList<>();

        // 2) 先把 RecipeGroupAndTool 直接轉成 OneConditionRecipeAndToolInfo
        //    condition, toolIdList, recipeId 對應即可
        for (RecipeGroupAndTool rgt : recipeGroupAndToolList) {
            OneConditionRecipeAndToolInfo info = OneConditionRecipeAndToolInfo.builder()
                    .condition(rgt.getCondition())       // 直接設定 condition
                    .toolIdList(rgt.getToolIdList())     // 直接設定 tool
                    .recipeId(rgt.getRecipeId())         // 直接設定 recipe
                    .build();

            result.add(info);
        }

        // 3) 再處理 MultipleRecipeData 部分
        // 3.1) 先依照 condition 分組
        Map<String, List<MultipleRecipeData>> groupedByCondition = multipleRecipeDataList.stream()
                .collect(Collectors.groupingBy(MultipleRecipeData::getCondition, LinkedHashMap::new, Collectors.toList()));

        for (Map.Entry<String, List<MultipleRecipeData>> entry : groupedByCondition.entrySet()) {
            String condVal = entry.getKey();               // e.g. "01", "02", ...
            List<MultipleRecipeData> thisConditionList = entry.getValue();

            // 3.2) 以 "RC_RECIPE_ID_XX" 的 XX 作為 key，再把同一 XX 下的
            //      (1) RC_RECIPE_ID_XX => RECIPE
            //      (2) RC_RECIPE_ID_XX_EQP_OA => TOOL
            //      放在一起
            Map<String, Map<String, String>> suffixMap = new LinkedHashMap<>();

            for (MultipleRecipeData mrd : thisConditionList) {
                String name = mrd.getName();   // 可能是 "RC_RECIPE_ID_01" 或 "M_FOLLOW_CHUCK_DEDICATION"

                // ★ 若不是預期 pattern，直接跳過，不納入後續組裝
                if (name == null || !name.startsWith("RC_RECIPE_ID_")) {
                    continue;
                }

                String val = mrd.getValue();
                String suffix = parseSuffixFromName(name);  // 取出 "01" ...

                // 若 suffix 為空字串代表格式有誤，也跳過
                if (suffix.isEmpty()) {
                    continue;
                }

                Map<String, String> typeMap = suffixMap.computeIfAbsent(suffix, k -> new HashMap<>());

                if (name.endsWith("_EQP_OA")) {
                    typeMap.put("TOOL", val);
                } else {
                    typeMap.put("RECIPE", val);
                }
            }

            // 3.3) 為該 condition 下，每個 suffix 都組成一筆 OneConditionRecipeAndToolInfo
            for (Map.Entry<String, Map<String, String>> suffixEntry : suffixMap.entrySet()) {
                String suffix = suffixEntry.getKey();             // e.g. "01"
                Map<String, String> typeMap = suffixEntry.getValue();

                // 組成最終的 condition (e.g. "01_M01")
                String finalCondition = condVal + "_M" + suffix;

                // RECIPE (若沒對應到，就給空字串)
                String recipeId = typeMap.getOrDefault("RECIPE", "");
                // TOOL (若沒對應到，就給空字串)
                String toolIdList = typeMap.getOrDefault("TOOL", "");

                OneConditionRecipeAndToolInfo info = OneConditionRecipeAndToolInfo.builder()
                        .condition(finalCondition)
                        .toolIdList(toolIdList)
                        .recipeId(recipeId)
                        .build();

                result.add(info);
            }
        }

        return result;
    }

    /**
     * 範例：將 "RC_RECIPE_ID_01" 或 "RC_RECIPE_ID_01_EQP_OA" 中的 "01" 取出
     * 若格式不同，請視情況調整。
     */
    private String parseSuffixFromName(String name) {
        final String prefix = "RC_RECIPE_ID_";
        if (!name.startsWith(prefix)) {
            return "";
        }
        // 先移除 "RC_RECIPE_ID_"
        String tmp = name.substring(prefix.length()); // 例如： "01_EQP_OA" 或 "01"
        // 如果含有 "_EQP_OA"，我們就只取前面那部分
        int idx = tmp.indexOf("_EQP_OA");
        if (idx >= 0) {
            return tmp.substring(0, idx);  // 只保留 "01"
        }
        return tmp; // 不含 "_EQP_OA" 就直接回傳
    }

    public List<RecipeGroupAndTool> getRecipeGroupAndTool(String runcardId) {
        Optional<List<RecipeGroupAndTool>> opt = runcardInfoDao.getRecipeGroupsAndToolInfos();
        return opt.orElseGet(ArrayList::new);
    }

    public List<MultipleRecipeData> getMultipleRecipeData(String runcardId) {
        Optional<List<MultipleRecipeData>> opt = runcardInfoDao.multipleRecipeData();
        return opt.orElseGet(ArrayList::new);
    }

    public List<ArrivalStatus> getRuncardArrivalStatuses(List<String> runcardIds) {
        if (runcardIds == null || runcardIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArrivalStatus> mockList = new ArrayList<>();
        for (String rcId : runcardIds) {
            ArrivalStatus status = ArrivalStatus.builder()
                    .runcardId(rcId)
                    .arrivalTime(LocalDateTime.now().toString())
                    .opeNo("OPE-10")
                    .build();
            mockList.add(status);
        }
        return mockList;
    }

    public List<IssuingEngineerInfo> getIssuingEngineerInfo(List<String> issuingEngineerIdList) {
        Optional<List<IssuingEngineerInfo>> engineerInfos = rcOwnerDao.issuingEngineerInfo(issuingEngineerIdList);
        return engineerInfos.orElseGet(ArrayList::new);
    }
}
