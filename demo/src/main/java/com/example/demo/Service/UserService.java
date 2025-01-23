package com.example.demo.Service;

import org.springframework.stereotype.Service;

@Service
public class UserService {

    public String validateSection(String sectionId) {
        // 模擬驗證邏輯
        if (sectionId == null || sectionId.isEmpty()) {
            throw new IllegalArgumentException("Section ID is invalid");
        }
        // 假設 sectionId 驗證通過後返回對應的部門名稱
        return "validated-section-" + sectionId;
    }
}

