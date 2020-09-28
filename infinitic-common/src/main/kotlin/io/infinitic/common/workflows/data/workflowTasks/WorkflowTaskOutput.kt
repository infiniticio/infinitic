package io.infinitic.common.workflows.data.workflowTasks

import io.infinitic.common.workflows.data.commands.NewCommand
import io.infinitic.common.workflows.data.methodRuns.MethodOutput
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.properties.Properties
import io.infinitic.common.workflows.data.properties.PropertyStore
import io.infinitic.common.workflows.data.steps.NewStep
import io.infinitic.common.workflows.data.workflows.WorkflowId

data class WorkflowTaskOutput(
    val workflowId: WorkflowId,
    val methodRunId: MethodRunId,
    val newCommands: List<NewCommand>,
    val newSteps: List<NewStep>,
    val workflowPropertiesUpdates: Properties,
    val workflowPropertyStoreUpdates: PropertyStore,
    val methodOutput: MethodOutput?
)
