package com.example.demo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ForwardProcess {
    private String lotId;
    private String preOpeNo;
    private String recipeId;
    private String toolId;
    private String claimTime;
    private String eqpCategory;
}

