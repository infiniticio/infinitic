package io.infinitic.common.workflowManager.data.workflowTasks

import io.infinitic.common.workflowManager.data.commands.NewCommand
import io.infinitic.common.workflowManager.data.methodRuns.MethodOutput
import io.infinitic.common.workflowManager.data.methodRuns.MethodRunId
import io.infinitic.common.workflowManager.data.properties.Properties
import io.infinitic.common.workflowManager.data.properties.PropertyStore
import io.infinitic.common.workflowManager.data.steps.NewStep
import io.infinitic.common.workflowManager.data.workflows.WorkflowId

data class WorkflowTaskOutput(
    val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val newCommands: List<NewCommand>,
    val newSteps: List<NewStep>,
    val workflowPropertiesUpdates: Properties,
    val workflowPropertyStoreUpdates: PropertyStore,
    val methodOutput: MethodOutput?
)
