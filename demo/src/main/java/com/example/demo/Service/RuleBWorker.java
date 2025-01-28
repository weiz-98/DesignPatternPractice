package com.example.demo.Service;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RuleBWorker implements Worker {

    private final String taskDefName;

    public RuleBWorker() {
        this.taskDefName = "RuleBWorker";
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

        boolean pass = toolId.endsWith("3");
        result.getOutputData().put("result", pass ? "pass" : "fail");

        result.getOutputData().put("processedBy", taskDefName);
        return result;
    }
}

