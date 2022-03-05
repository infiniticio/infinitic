/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.common.workflows.data.workflowTasks

import io.infinitic.common.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engines.messages.DispatchTask
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.tasks.TaskOptions
import io.infinitic.workflows.WorkflowOptions
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTaskParameters(
    val taskId: TaskId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    val workflowOptions: WorkflowOptions,
    val workflowTags: Set<WorkflowTag>,
    val workflowMeta: WorkflowMeta,
    val workflowPropertiesHashValue: Map<PropertyHash, PropertyValue>,
    val workflowTaskIndex: WorkflowTaskIndex,
    val methodRun: MethodRun,
    val emitterName: ClientName
) {
    fun toDispatchTaskMessage() = DispatchTask(
        taskName = TaskName(WorkflowTask::class.java.name),
        taskId = taskId,
        taskOptions = TaskOptions(),
        clientWaiting = false,
        methodName = MethodName(WorkflowTask::handle.name),
        methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskParameters::class.java.name)),
        methodParameters = MethodParameters.from(this),
        workflowId = workflowId,
        workflowName = workflowName,
        methodRunId = methodRun.methodRunId,
        taskTags = setOf(),
        taskMeta = TaskMeta(),
        emitterName = emitterName
    )
}
