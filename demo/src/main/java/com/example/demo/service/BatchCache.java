package com.example.demo.service;

import com.example.demo.po.*;
import com.example.demo.vo.OneConditionRecipeAndToolInfo;
import com.example.demo.vo.RecipeGroupAndTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一個「單批次」(prototype) 生命週期的快取。
 * <p>
 * ● 與 {@code RuncardFlowService.processRuncardBatch(..)} 同生同滅。
 * ● 完全 thread-safe（ConcurrentHashMap）。
 * ● 不做 TTL；批次跑完即釋放。
 */
@Slf4j
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class BatchCache {

    private final DataLoaderService dataLoaderService;

    private Map<String, String> toolSectMap;

    public Map<String, String> getToolIdToSectNameMap() {
        if (toolSectMap == null) {
            toolSectMap = dataLoaderService.getToolIdToSectNameMap();
            log.debug("[BatchCache] load toolSectMap size={}", toolSectMap.size());
        }
        return toolSectMap;
    }

    private final Map<String, List<OneConditionRecipeAndToolInfo>> oneRuncardRecipeAndToolInfosCache = new ConcurrentHashMap<>();
    private final Map<String, List<ForwardProcess>> forwardProcessCache = new ConcurrentHashMap<>();
    private final Map<String, List<InhibitionCheckStatus>> inhibitionCheckStatusCache = new ConcurrentHashMap<>();
    private final Map<String, WaferCondition> waferConditionCache = new ConcurrentHashMap<>();
    private final Map<String, List<RecipeGroupCheckBlue>> recipeGroupCheckBlueCache = new ConcurrentHashMap<>();
    private final Map<String, List<RecipeGroupAndTool>> recipeGroupAndToolCache = new ConcurrentHashMap<>();
    private final Map<String, List<IssuingEngineerInfo>> engineerCache = new ConcurrentHashMap<>();

    public List<OneConditionRecipeAndToolInfo> getRecipeAndToolInfo(String rcId) {
        return oneRuncardRecipeAndToolInfosCache.computeIfAbsent(rcId, id -> {
            List<OneConditionRecipeAndToolInfo> lst = dataLoaderService.getRecipeAndToolInfo(id);
            log.debug("[BatchCache] load recipeAndTool rc={}, size={}", id, lst.size());
            return lst;
        });
    }

    public List<ForwardProcess> getForwardProcess(String rcId) {
        return forwardProcessCache.computeIfAbsent(rcId, id -> {
            List<ForwardProcess> lst = dataLoaderService.getForwardProcess(id);
            log.debug("[BatchCache] load forwardProcess rc={}, size={}", id, lst.size());
            return lst;
        });
    }

    public List<InhibitionCheckStatus> getInhibitionCheckStatus(String rcId) {
        return inhibitionCheckStatusCache.computeIfAbsent(rcId, id -> {
            List<InhibitionCheckStatus> lst = dataLoaderService.getInhibitionCheckStatus(id);
            log.debug("[BatchCache] load inhibitionStatus rc={}, size={}", id, lst.size());
            return lst;
        });
    }

    public WaferCondition getWaferCondition(String rcId) {
        return waferConditionCache.computeIfAbsent(rcId, id -> {
            WaferCondition wc = dataLoaderService.getWaferCondition(id);
            log.debug("[BatchCache] load waferCondition rc={}, null?={}", id, wc == null);
            return wc;
        });
    }

    /**
     * key = recipeGroupId + '#' + sorted(toolIds)
     */
    public List<RecipeGroupCheckBlue> getRecipeGroupCheckBlue(String recipeGroupId, List<String> toolIds) {
        String k = recipeGroupId + "#" + String.join(",", new TreeSet<>(toolIds));
        return recipeGroupCheckBlueCache.computeIfAbsent(k, key -> {
            List<RecipeGroupCheckBlue> lst = dataLoaderService.getRecipeGroupCheckBlue(recipeGroupId, toolIds);
            log.debug("[BatchCache] load blue recipeGroupId={}, toolIds={}, size={}",
                    recipeGroupId, toolIds, lst.size());
            return lst;
        });
    }

    public List<RecipeGroupAndTool> getRecipeGroupAndTool(String rcId) {
        return recipeGroupAndToolCache.computeIfAbsent(rcId, id -> {
            List<RecipeGroupAndTool> list = dataLoaderService.getRecipeGroupAndTool(id);
            log.debug("[BatchCache] load recipeGroupAndTool rc={}, size={}", id, list.size());
            return list;
        });
    }

    public List<IssuingEngineerInfo> getIssuingEngineerInfo(List<String> empIds) {
        String key = String.join(",", new TreeSet<>(empIds));
        return engineerCache.computeIfAbsent(key, k -> {
            List<IssuingEngineerInfo> lst = dataLoaderService.getIssuingEngineerInfo(empIds);
            log.debug("[BatchCache] load engineerIds={}, size={}", empIds, lst.size());
            return lst;
        });
    }
}

