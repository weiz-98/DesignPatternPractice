package com.example.demo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecipeGroupCheckBlue {
    private String toolId;
    private String chamberId;
    private String releaseFlag;
    private String enableFlag;
}
