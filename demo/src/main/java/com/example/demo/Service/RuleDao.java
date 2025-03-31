package com.example.demo.Service;

import com.example.demo.po.ForwardProcess;
import com.example.demo.po.InhibitionCheckStatus;
import com.example.demo.po.RecipeGroupCheckBlue;
import com.example.demo.po.WaferCondition;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class RuleDao {

    public Optional<List<ForwardProcess>> getForwardProcess() {
        List<ForwardProcess> list = new ArrayList<>();
        // 假資料示例
        list.add(ForwardProcess.builder()
                .lotId("LOT001")
                .preOpeNo("001")
                .recipeId("RCP001")
                .toolId("TOOL001")
                .claimTime("2023-04-01T10:00:00")
                .eqpCategory("CAT1")
                .build());
        list.add(ForwardProcess.builder()
                .lotId("LOT002")
                .preOpeNo("002")
                .recipeId("RCP002")
                .toolId("TOOL002")
                .claimTime("2023-04-01T11:00:00")
                .eqpCategory("CAT2")
                .build());
        return Optional.of(list);
    }

    public Optional<List<InhibitionCheckStatus>> getInhibitionCheckStatus() {
        List<InhibitionCheckStatus> list = new ArrayList<>();
        // 假資料示例
        list.add(InhibitionCheckStatus.builder()
                .inhibitFlag("N")
                .build());
        list.add(InhibitionCheckStatus.builder()
                .inhibitFlag("Y")
                .build());
        return Optional.of(list);
    }

    public Optional<List<RecipeGroupCheckBlue>> getRecipeGroupCheckBlue() {
        List<RecipeGroupCheckBlue> list = new ArrayList<>();
        // 假資料示例
        list.add(RecipeGroupCheckBlue.builder()
                .toolId("TOOL001")
                .chamberId("CH001")
                .releaseFlag("Y")
                .enableFlag("Y")
                .build());
        list.add(RecipeGroupCheckBlue.builder()
                .toolId("TOOL002")
                .chamberId("CH002")
                .releaseFlag("N")
                .enableFlag("Y")
                .build());
        return Optional.of(list);
    }

    public Optional<WaferCondition> getWaferCondition() {
        // 這裡只建立一筆假資料並返回
        WaferCondition waferCondition = WaferCondition.builder()
                .uniqueCount("10")
                .wfrQty("100")
                .build();
        return Optional.of(waferCondition);
    }
}

