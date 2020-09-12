package io.infinitic.workflowManager.common.data.workflowTasks

import io.infinitic.workflowManager.common.data.commands.PastCommand
import io.infinitic.workflowManager.common.data.properties.PropertyValue
import io.infinitic.workflowManager.common.data.properties.PropertyName

data class WorkflowTaskOutput(
        val commands: List<PastCommand> = listOf(),
        val updatedProperties: Map<PropertyName, PropertyValue> = mapOf()
)
