package io.infinitic.workflowManager.common.states

import io.infinitic.workflowManager.common.avro.AvroConverter
import io.infinitic.workflowManager.common.data.decisions.DecisionId
import io.infinitic.workflowManager.common.data.WorkflowId
import io.infinitic.workflowManager.common.data.branches.Branch
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.messages.ForWorkflowEngineMessage

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
