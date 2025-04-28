package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RuncardRawInfo {
    private String runcardId;
    private String issuingEngineer;
    private String lotId;
    private String partId;
    private String status;
    private String purpose;
    private String supervisorAndDepartment;
    private Integer numberOfPieces;
    private String holdAtOperNo;
}


