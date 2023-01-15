/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined below, subject to
 * the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the License will not
 * include, and the License does not grant to you, the right to Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights granted to you
 * under the License to provide to third parties, for a fee or other consideration (including
 * without limitation fees for hosting or consulting/ support services related to the Software), a
 * product or service whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also include this
 * Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */
package io.infinitic.common.workflows.data.workflowTasks

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.methodRuns.MethodRun
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTaskParameters(
    @AvroDefault("0.9.7") val version: String = io.infinitic.version,
    val taskId: TaskId,
    val workflowId: WorkflowId,
    val workflowName: WorkflowName,
    @AvroDefault(Avro.NULL) val workflowVersion: WorkflowVersion?,
    val workflowTags: Set<WorkflowTag>,
    val workflowMeta: WorkflowMeta,
    val workflowPropertiesHashValue: Map<PropertyHash, PropertyValue>,
    val workflowTaskIndex: WorkflowTaskIndex,
    val methodRun: MethodRun,
    val emitterName: ClientName
) {
  fun toExecuteTaskMessage() =
      ExecuteTask(
          serviceName = ServiceName(WorkflowTask::class.java.name),
          taskId = taskId,
          clientWaiting = false,
          methodName = MethodName(WorkflowTask::handle.name),
          methodParameterTypes =
              MethodParameterTypes(listOf(WorkflowTaskParameters::class.java.name)),
          methodParameters = MethodParameters.from(this),
          workflowId = workflowId,
          workflowName = workflowName,
          workflowVersion = workflowVersion,
          methodRunId = methodRun.methodRunId,
          lastError = null,
          taskRetryIndex = TaskRetryIndex(0),
          taskRetrySequence = TaskRetrySequence(0),
          taskTags = setOf(),
          taskMeta = TaskMeta(),
          emitterName = emitterName)
}
