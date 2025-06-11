package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuleExecutionContext {
    private String cond;
    private RuncardRawInfo runcardRawInfo;
    private RecipeToolPair recipeToolPair;
}

