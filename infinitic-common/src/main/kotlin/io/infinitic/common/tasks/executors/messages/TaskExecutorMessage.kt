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
package io.infinitic.common.tasks.executors.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.ClientName
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetryIndex
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.errors.ExecutionError
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class TaskExecutorMessage : Message {
  abstract val taskId: TaskId
  abstract val serviceName: ServiceName
  abstract val emitterName: ClientName

  override fun envelope() = TaskExecutorEnvelope.from(this)

  fun isWorkflowTask() = serviceName == ServiceName(WorkflowTask::class.java.name)
}

@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
data class ExecuteTask(
    @SerialName("taskName") override val serviceName: ServiceName,
    override val taskId: TaskId,
    override val emitterName: ClientName,
    val clientWaiting: Boolean,
    val methodName: MethodName,
    val methodParameterTypes: MethodParameterTypes?,
    val methodParameters: MethodParameters,
    val taskRetrySequence: TaskRetrySequence,
    val taskRetryIndex: TaskRetryIndex,
    val lastError: ExecutionError?,
    val workflowId: WorkflowId?,
    val workflowName: WorkflowName?,
    @AvroDefault(Avro.NULL) val workflowVersion: WorkflowVersion?,
    val methodRunId: MethodRunId?,
    val taskTags: Set<TaskTag>,
    val taskMeta: TaskMeta
) : TaskExecutorMessage()
