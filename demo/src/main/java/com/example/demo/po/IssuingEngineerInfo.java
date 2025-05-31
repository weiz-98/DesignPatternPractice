package com.example.demo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IssuingEngineerInfo {
    private String engineerName;
    private String engineerId;
    private String divisionName;
    private String divisionId;
    private String departmentName;
    private String departmentId;
    private String sectionName;
    private String sectionId;
}
