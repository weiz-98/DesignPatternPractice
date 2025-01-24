package com.example.demo.Repository;

import com.example.demo.vo.Rule;
import com.example.demo.vo.ToolGroup;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ToolGroupDao {

    public List<ToolGroup> getToolGroups() {
        // 模擬資料，假設兩個 Tool Group
        return List.of(
                new ToolGroup("group1", List.of("tool1", "tool2"),
                        List.of(new Rule("ruleA", "group1"), new Rule("ruleB", "group1"))),
                new ToolGroup("group2", List.of("tool3"),
                        List.of(new Rule("ruleA", "group2")))
        );
    }
}


