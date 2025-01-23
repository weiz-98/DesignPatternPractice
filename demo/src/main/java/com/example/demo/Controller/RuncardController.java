package com.example.demo.Controller;

import com.example.demo.Service.RuncardService;
import com.example.demo.vo.RuncardRequest;
import com.example.demo.vo.RuncardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/runcard")
public class RuncardController {

    private final RuncardService runcardService;

    @PostMapping("/validate")
    public ResponseEntity<List<RuncardResponse>> validateRuncards(@RequestBody RuncardRequest request) {
        List<RuncardResponse> response = runcardService.validateRuncards(request);
        return ResponseEntity.ok(response);
    }
}

