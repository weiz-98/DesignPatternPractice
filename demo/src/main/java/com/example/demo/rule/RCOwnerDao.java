package com.example.demo.rule;

import com.example.demo.po.IssuingEngineerInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Component
public class RCOwnerDao {

    /** -------------------------  靜態假資料  ------------------------- */
    private static final List<IssuingEngineerInfo> MOCK_DATA = List.of(
            IssuingEngineerInfo.builder()
                    .engineerId("E001").engineerName("Alice")
                    .divisionId("DV01").divisionName("Fab Division")
                    .departmentId("DP01").departmentName("Process Dept")
                    .sectionId("SC01").sectionName("Litho Section")
                    .build(),

            IssuingEngineerInfo.builder()
                    .engineerId("E002").engineerName("Bob")
                    .divisionId("DV01").divisionName("Fab Division")
                    .departmentId("DP02").departmentName("Equipment Dept")
                    .sectionId("SC02").sectionName("Etch Section")
                    .build(),

            IssuingEngineerInfo.builder()
                    .engineerId("E003").engineerName("Charlie")
                    .divisionId("DV02").divisionName("R&D Division")
                    .departmentId("DP03").departmentName("Module Dev Dept")
                    .sectionId("SC03").sectionName("CMP Section")
                    .build()
    );
    /** -------------------------------------------------------------- */

    /**
     * 回傳「發卡工程師」基本資訊的假資料。
     *
     * @param issuingEngineerIdList 需要查詢的員工 ID 清單
     * @return 若輸入為 null / 空，回傳 {@link Optional#empty()}；否則回傳對應之
     *         {@link IssuingEngineerInfo} 清單 (未找到者自動忽略)。
     */
    public Optional<List<IssuingEngineerInfo>> issuingEngineerInfo(List<String> issuingEngineerIdList) {

        if (issuingEngineerIdList == null || issuingEngineerIdList.isEmpty()) {
            log.info("[issuingEngineerInfo] empty id list – return empty Optional");
            return Optional.empty();
        }

        List<IssuingEngineerInfo> matched = MOCK_DATA.stream()
                .filter(info -> issuingEngineerIdList.contains(info.getEngineerId()))
                .collect(Collectors.toList());

        log.info("[issuingEngineerInfo] ids={}, matchedRows={}", issuingEngineerIdList, matched.size());
        return matched.isEmpty() ? Optional.empty() : Optional.of(matched);
    }
}
