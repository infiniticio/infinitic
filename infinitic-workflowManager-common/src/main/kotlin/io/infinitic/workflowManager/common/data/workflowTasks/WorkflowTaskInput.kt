package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.methods.MethodId
import io.infinitic.workflowManager.common.data.instructions.PastInstruction
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methods.Method
import io.infinitic.workflowManager.common.data.methods.MethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions

data class WorkflowTaskInput(
    val workflowName: WorkflowName,
    val workflowId: WorkflowId,
    val workflowOptions: WorkflowOptions,

    val methodId: MethodId,
    val method: Method,
    val methodInput: MethodInput,

    val workflowMethodPropertiesAtStart: Properties,
    val workflowPropertyStore: PropertyStore,
    val workflowMethodPastInstructions: List<PastInstruction>
)
