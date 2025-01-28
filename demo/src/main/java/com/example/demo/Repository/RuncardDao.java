package com.example.demo.Repository;

import com.example.demo.vo.Runcard;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class RuncardDao {

    public List<Runcard> getRuncards(String startTime, String endTime, String sectionId) {
        // 模擬資料，假設兩張 Runcard
        return List.of(
                new Runcard("runcard1", "tool1", "2025-01-01", "approver1"),
                new Runcard("runcard2", "tool3", "2025-01-02", "approver2")
        );
    }
}


