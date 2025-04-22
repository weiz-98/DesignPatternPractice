package com.example.demo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RuncardParsingRequest {
    private List<String> sectionIds;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
