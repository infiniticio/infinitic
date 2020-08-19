package io.infinitic.workflowManager.engine.states

import io.infinitic.workflowManager.engine.avroConverter.AvroConverter
import io.infinitic.workflowManager.engine.data.decisions.DecisionId
import io.infinitic.workflowManager.engine.data.WorkflowId
import io.infinitic.workflowManager.engine.data.branches.Branch
import io.infinitic.workflowManager.engine.data.properties.Properties
import io.infinitic.workflowManager.engine.data.properties.PropertyStore
import io.infinitic.workflowManager.engine.messages.ForWorkflowEngineMessage

sealed class State

data class WorkflowEngineState(
    val workflowId: WorkflowId,
    var parentWorkflowId: WorkflowId? = null,
    var ongoingDecisionId: DecisionId? = null,
    val bufferedMessages: MutableList<ForWorkflowEngineMessage> = mutableListOf(),
    val store: PropertyStore = PropertyStore(mutableMapOf()),
    val runningBranches: MutableList<Branch> = mutableListOf(),
    val currentProperties: Properties = Properties(mapOf())
) : State() {
    fun deepCopy() = AvroConverter.fromStorage(AvroConverter.toStorage(this))
}
