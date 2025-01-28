package com.example.demo.Service;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

// Rule 的判定邏輯寫在 worker
public class RuleAWorker implements Worker {

    private final String taskDefName;

    public RuleAWorker() {
        this.taskDefName = "RuleAWorker";
    }

    @Override
    public String getTaskDefName() {
        return taskDefName;
    }

    @Override
    public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        String toolId = task.getInputData().get("toolId").toString();

        boolean pass = toolId.startsWith("A");
        result.getOutputData().put("result", pass ? "pass" : "fail");

        result.getOutputData().put("processedBy", taskDefName);
        return result;
    }
}
