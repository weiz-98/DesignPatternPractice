package com.example.demo.controller;

import com.example.demo.service.RuncardService;
import com.example.demo.vo.RuncardParsingRequest;
import com.example.demo.vo.RuncardParsingResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/runcards")
@RequiredArgsConstructor
public class RuncardController {

    private final RuncardService runcardService;

    @PostMapping("/refresh")
    public ResponseEntity<List<RuncardParsingResult>> refreshRuncardBatch(
            @Valid @RequestBody RuncardParsingRequest request) {
        List<RuncardParsingResult> results = runcardService.refresh(request);

        return ResponseEntity.ok(results);
    }
}
