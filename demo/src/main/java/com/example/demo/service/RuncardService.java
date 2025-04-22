package com.example.demo.service;

import com.example.demo.vo.RuncardParsingRequest;
import com.example.demo.vo.RuncardParsingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RuncardService {
    private final RuncardFlowService runcardFlowService;

    public List<RuncardParsingResult> refresh(RuncardParsingRequest runcardParsingRequest){
        runcardFlowService.processRuncardBatch(runcardParsingRequest);
        return List.of(RuncardParsingResult.builder().build());
    }
}
