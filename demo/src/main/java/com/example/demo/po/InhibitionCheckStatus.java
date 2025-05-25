package com.example.demo.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InhibitionCheckStatus {
    private String condition;
    private String inhibitFlag;
}
