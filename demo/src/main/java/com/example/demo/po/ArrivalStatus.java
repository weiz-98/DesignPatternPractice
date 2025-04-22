package com.example.demo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ArrivalStatus {
    private String runcardId;
    private String arrivalTime;
    private String opeNo;
}
