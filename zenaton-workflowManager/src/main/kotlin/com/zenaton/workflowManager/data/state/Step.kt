package com.zenaton.workflowManager.data.state

import com.fasterxml.jackson.annotation.JsonIgnore
import com.zenaton.jobManager.data.JobId
import com.zenaton.workflowManager.data.DelayId
import com.zenaton.workflowManager.data.EventId
import com.zenaton.workflowManager.data.WorkflowId

data class Step(
    val stepHash: StepHash,
    val criterion: StepCriterion,
    var propertiesAfterCompletion: Properties?
) {
    @JsonIgnore
    fun isCompleted() = criterion.isCompleted()

    fun completeTask(jobId: JobId, properties: Properties): Boolean {
        return complete(ActionId(jobId), properties)
    }

    fun completeChildWorkflow(workflowId: WorkflowId, properties: Properties): Boolean {
        return complete(ActionId(workflowId), properties)
    }

    fun completeDelay(delayId: DelayId, properties: Properties): Boolean {
        return complete(ActionId(delayId), properties)
    }

    fun completeEvent(eventId: EventId, properties: Properties): Boolean {
        return complete(ActionId(eventId), properties)
    }

    private fun complete(actionId: ActionId, properties: Properties): Boolean {
        if (! isCompleted()) {
            criterion.complete(actionId)
            if (criterion.isCompleted()) {
                propertiesAfterCompletion = properties.copy()
                return true
            }
        }
        return false
    }
}
