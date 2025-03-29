package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuncardRawInfo {
    private String runcardId;
    private String partId;
    private String opeNo;
    private String arriveAt;
    private String createTime;
    private String approvedManager;
    private String createdEngineer;
}


