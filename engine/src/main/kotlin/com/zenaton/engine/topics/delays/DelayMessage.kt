package com.zenaton.engine.topics.delays

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.zenaton.engine.data.delays.DelayId
import com.zenaton.engine.data.workflows.WorkflowId

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(value = DelayDispatched::class, name = "DelayDispatched"),
    JsonSubTypes.Type(value = DelayCompleted::class, name = "DelayCompleted")
)
sealed class DelayMessage(open var delayId: DelayId) {
    @JsonIgnore
    fun getStateKey() = delayId.id
}

data class DelayDispatched(
    override var delayId: DelayId,
    val workflowId: WorkflowId
) : DelayMessage(delayId)

data class DelayCompleted(
    override var delayId: DelayId,
    val workflowId: WorkflowId
) : DelayMessage(delayId)
