package com.example.demo.Service;

import com.example.demo.Repository.RuncardDao;
import com.example.demo.Repository.ToolGroupDao;
import com.example.demo.vo.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class RuncardService {

    private final UserService userService;
    private final RuncardDao runcardDao;
    private final ToolGroupDao toolGroupDao;
    private final DataLoaderService dataLoaderService;
    private final RuncardHandlerService runcardHandlerService;

    public List<RuncardResponse> validateRuncards(RuncardRequest request) {
        // 1. 驗證使用者 section
        String sectionId = userService.validateSection(request.getSectionId());

        // 2. 從 Siview DB 拿到 runcards
        List<Runcard> runcards = runcardDao.getRuncards(request.getStartTime(), request.getEndTime(), sectionId);

        // 3. 從 MongoDB 拿到 tool groups
        List<ToolGroup> toolGroups = toolGroupDao.getToolGroups();

        // 4. DataLoaderService 組出 Map<runcard, rules>
        Map<Runcard, List<Rule>> runcardRuleMap = dataLoaderService.loadRuncardRules(runcards, toolGroups);

        // 5. RuncardHandlerService 處理每張 runcard 的判斷
        return runcardHandlerService.processRuncards(runcardRuleMap);
    }
}

