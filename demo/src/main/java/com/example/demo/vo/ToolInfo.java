package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ToolInfo {
    private String DeptName;
    private String SecName;
    private String toolId;
    private String chamberId;
}
