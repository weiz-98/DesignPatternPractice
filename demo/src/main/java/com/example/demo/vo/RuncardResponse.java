package com.example.demo.vo;

import lombok.Data;

import java.util.List;
@Data
public class RuncardResponse {
    private String runcardId;
    private List<Result> results;

    @Data
    public static class Result {
        private String toolId;
        private String toolGroupName;
        private String rule;
        private String result; // pass / fail / not arrive
    }
}

