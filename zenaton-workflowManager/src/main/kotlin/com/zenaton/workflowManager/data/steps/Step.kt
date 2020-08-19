package com.zenaton.workflowManager.data.steps

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.jobManager.common.data.JobId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventId
import com.zenaton.workflowManager.data.WorkflowId
import com.zenaton.workflowManager.data.commands.CommandId
import com.zenaton.workflowManager.data.properties.Properties

data class Step(
    val stepHash: StepHash,
    val criterion: StepCriterion,
    var propertiesAfterCompletion: Properties
) {
    @JsonIgnore
    fun isCompleted() = criterion.isCompleted()

    fun completeTask(jobId: JobId, properties: Properties): Boolean {
        return complete(CommandId(jobId), properties)
    }

    fun completeChildWorkflow(workflowId: WorkflowId, properties: Properties): Boolean {
        return complete(CommandId(workflowId), properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        return complete(CommandId(delayId), properties)
    }

    fun completeEvent(eventId: EventId, properties: Properties): Boolean {
        return complete(CommandId(eventId), properties)
    }

    private fun complete(commandId: CommandId, properties: Properties): Boolean {
        if (! isCompleted()) {
            criterion.complete(commandId)
            if (criterion.isCompleted()) {
                propertiesAfterCompletion = properties.copy()
                return true
            }
        }
        return false
    }
}
