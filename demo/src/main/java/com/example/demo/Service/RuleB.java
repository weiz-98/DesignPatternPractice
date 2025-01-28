package com.example.demo.Service;

import com.example.demo.vo.Rule;
import com.example.demo.vo.Runcard;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RuleB implements IRuleCheck {

    private final Worker worker;

    public RuleB() {
        this.worker = new RuleBWorker(); /// 注入對應的 Worker
    }

    @Override
    public String execute(Runcard runcard, Rule rule) {
        Task task = new Task();
        task.setTaskDefName(worker.getTaskDefName());
        task.getInputData().put("toolId", runcard.getToolId());
        task.getInputData().put("ruleName", rule.getName());

        TaskResult result = worker.execute(task);
        return result.getOutputData().getOrDefault("result", "fail").toString();
    }
}
