package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
// RuleA 主要實作建立 worker 的流程
public class RuleA implements IRuleCheck {

    private final Worker worker;

    public RuleA() {
        this.worker = new RuleAWorker(); // 注入對應的 Worker
    }

    @Override
    public String execute(Runcard runcard, Rule rule) {
        // 建立 Conductor Task 並設定輸入數據
        Task task = new Task();
        task.setTaskDefName(worker.getTaskDefName());
        task.getInputData().put("toolId", runcard.getToolId());
        task.getInputData().put("ruleName", rule.getName());

        TaskResult result = worker.execute(task);
        return result.getOutputData().getOrDefault("result", "fail").toString();
    }
}
