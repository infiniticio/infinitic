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
package io.infinitic.common.workflows.engine.state

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.serDe.avro.AvroSerDe
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workers.config.WorkflowVersion
import io.infinitic.common.workflows.data.channels.ReceivingChannel
import io.infinitic.common.workflows.data.channels.SignalId
import io.infinitic.common.workflows.data.commands.CommandId
import io.infinitic.common.workflows.data.commands.CommandStatus
import io.infinitic.common.workflows.data.commands.PastCommand
import io.infinitic.common.workflows.data.properties.PropertyHash
import io.infinitic.common.workflows.data.properties.PropertyName
import io.infinitic.common.workflows.data.properties.PropertyValue
import io.infinitic.common.workflows.data.workflowMethods.PositionInWorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethod
import io.infinitic.common.workflows.data.workflowMethods.WorkflowMethodId
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskIndex
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.messages.WorkflowStateEngineMessage
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val prettyPrint = Json { prettyPrint = true }

@Serializable
@AvroNamespace("io.infinitic.workflows.engine")
data class WorkflowState(
  /** Id of last handled message (used to ensure idempotency) */
  var lastMessageId: MessageId,

  /** Id of this workflow instance */
  val workflowId: WorkflowId,

  /** Workflow's name (used wy worker to instantiate) */
  val workflowName: WorkflowName,

  /** Workflow's version */
  @AvroDefault(Avro.NULL) var workflowVersion: WorkflowVersion?,

  /** Instance's tags defined when dispatched */
  val workflowTags: Set<WorkflowTag>,

  /** Instance's meta defined when dispatched */
  val workflowMeta: WorkflowMeta,

  /** Id of WorkflowTask currently running */
  var runningWorkflowTaskId: TaskId? = null,

  /** MethodRunId of WorkflowTask currently running */
  @AvroName("runningMethodRunId") var runningWorkflowMethodId: WorkflowMethodId? = null,

  /** Position of the step that triggered WorkflowTask currently running */
  @AvroName("runningMethodRunPosition") var positionInRunningWorkflowMethod: PositionInWorkflowMethod? = null,

  /**
   * In some situations, we know that multiples branches must be processed. As WorkflowTask
   * handles branch one by one, we orderly buffer these branches here (it happens when a
   * terminated command completes currentStep on multiple MethodRuns)
   */
  val runningTerminatedCommands: MutableList<CommandId> = mutableListOf(),

  /** Instant when WorkflowTask currently running was triggered */
  var runningWorkflowTaskInstant: MillisInstant? = null,

  /** Incremental index counting WorkflowTasks (used as an index for instance's state) */
  var workflowTaskIndex: WorkflowTaskIndex = WorkflowTaskIndex(0),

  /** Methods currently running. Once completed this data can be deleted to limit memory usage */
  @AvroName("methodRuns") val workflowMethods: MutableList<WorkflowMethod>,

  /** Ordered list of channels currently receiving */
  val receivingChannels: MutableList<ReceivingChannel> = mutableListOf(),

  /** Current (last) hash of instance's properties. hash is used as an index to actual value */
  val currentPropertiesNameHash: MutableMap<PropertyName, PropertyHash> = mutableMapOf(),

  /**
   * Store containing values of past and current values of properties (past values are useful when
   * replaying WorkflowTask)
   */
  var propertiesHashValue: MutableMap<PropertyHash, PropertyValue> = mutableMapOf(),

  /**
   * Messages received while a WorkflowTask is still running. They can not be handled immediately,
   * so we store them in this buffer
   */
  val messagesBuffer: MutableList<WorkflowStateEngineMessage> = mutableListOf()
) {

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, WorkflowState::class)
  }

  fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  fun toJson(): String = prettyPrint.encodeToString(serializer(), this)

  fun hasSignalAlreadyBeenReceived(signalId: SignalId) =
      workflowMethods.any { methodRun ->
        methodRun.pastCommands.any {
          it.commandStatus is CommandStatus.Completed &&
              (it.commandStatus as CommandStatus.Completed).signalId == signalId
        }
      }

  fun getRunningWorkflowMethod(): WorkflowMethod =
      workflowMethods.first { it.workflowMethodId == runningWorkflowMethodId }

  fun getWorkflowMethod(workflowMethodId: WorkflowMethodId) =
      workflowMethods.firstOrNull { it.workflowMethodId == workflowMethodId }

  fun getPastCommand(commandId: CommandId, workflowMethod: WorkflowMethod): PastCommand? =
      workflowMethod.getPastCommand(commandId)
      // if we do not find in this methodRun, then search within others
        ?: workflowMethods.map { it.getPastCommand(commandId) }.firstOrNull { it != null }

  fun removeWorkflowMethod(workflowMethod: WorkflowMethod) {
    workflowMethods.remove(workflowMethod)

    // clean receivingChannels once deleted
    receivingChannels.removeAll { it.workflowMethodId == workflowMethod.workflowMethodId }

    // clean properties
    removeUnusedPropertyHash()
  }

  fun removeWorkflowMethods() {
    workflowMethods.clear()

    // clean receivingChannels once deleted
    receivingChannels.clear()

    // clean properties
    removeUnusedPropertyHash()
  }

  /**
   * After completion or cancellation of methodRuns, we can clean up the properties not used anymore
   */
  private fun removeUnusedPropertyHash() {
    // set of all hashes still in use
    val propertyHashes = mutableSetOf<PropertyHash>()

    workflowMethods.forEach { workflowMethod ->
      workflowMethod.pastSteps.forEach { pastStep ->
        pastStep.propertiesNameHashAtTermination?.forEach { propertyHashes.add(it.value) }
      }
      workflowMethod.propertiesNameHashAtStart.forEach { propertyHashes.add(it.value) }
    }
    currentPropertiesNameHash.forEach { propertyHashes.add(it.value) }

    // remove all propertyHashValue not in use anymore
    propertiesHashValue.keys
        .filter { it !in propertyHashes }
        .forEach { propertiesHashValue.remove(it) }
  }
}
