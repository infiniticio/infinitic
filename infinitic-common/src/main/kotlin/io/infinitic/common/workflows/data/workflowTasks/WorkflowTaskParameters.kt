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
import com.github.avrokotlin.avro4k.AvroName
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskRetrySequence
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.TaskDispatchedEvent
import io.infinitic.common.workflows.engine.messages.data.TaskDispatched
import io.infinitic.currentVersion
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTaskParameters(
  @AvroName("version") @AvroDefault("0.9.7") val infiniticVersion: String = currentVersion,
  val taskId: TaskId,
  val workflowId: WorkflowId,
  val workflowName: WorkflowName,
  @AvroDefault(Avro.NULL) val workflowVersion: WorkflowVersion? = null,
  val workflowTags: Set<WorkflowTag>,
  val workflowMeta: WorkflowMeta,
  val workflowPropertiesHashValue: Map<PropertyHash, PropertyValue>,
  val workflowTaskIndex: WorkflowTaskIndex,
    // this property was added in 0.13.0
  @AvroDefault(Avro.NULL) val workflowTaskInstant: MillisInstant? = null,
    // Since 0.12.1 WorkflowTaskParameters is serialized using Avro, before that it was in JSON-Kotlin
  @AvroName("methodRun") @SerialName("methodRun") val workflowMethod: WorkflowMethod,
  val emitterName: EmitterName,
) {
  fun workflowTaskDispatchedEvent(emitterName: EmitterName) = TaskDispatchedEvent(
      taskDispatched = TaskDispatched(
          taskId = taskId,
          methodName = MethodName(WorkflowTask::handle.name),
          methodParameterTypes = MethodParameterTypes(listOf(WorkflowTaskParameters::class.java.name)),
          methodParameters = MethodParameters.from(this),
          serviceName = WorkflowTask.WORKFLOW_SERVICE_NAME,
          taskTags = setOf(),
          taskMeta = TaskMeta(),
          taskRetrySequence = TaskRetrySequence(),
          timeoutInstant = null,
      ),
      workflowName = workflowName,
      workflowId = workflowId,
      workflowVersion = workflowVersion,
      workflowMethodName = workflowMethod.methodName,
      workflowMethodId = workflowMethod.workflowMethodId,
      emitterName = emitterName,
  )


  fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, WorkflowTaskParameters::class)
  }

}
