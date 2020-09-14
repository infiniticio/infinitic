package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.commands.NewCommand
import io.infinitic.workflowManager.common.data.methodRuns.MethodOutput
import io.infinitic.workflowManager.common.data.methodRuns.MethodRunId
import io.infinitic.workflowManager.common.data.properties.Properties
import io.infinitic.workflowManager.common.data.properties.PropertyStore
import io.infinitic.workflowManager.common.data.steps.NewStep
import io.infinitic.workflowManager.common.data.workflows.WorkflowId

data class WorkflowTaskOutput(
    val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val newCommands: List<NewCommand>,
    val newSteps: List<NewStep>,
    val workflowPropertiesUpdates: Properties,
    val workflowPropertyStoreUpdates: PropertyStore,
    val methodOutput: MethodOutput?
)
