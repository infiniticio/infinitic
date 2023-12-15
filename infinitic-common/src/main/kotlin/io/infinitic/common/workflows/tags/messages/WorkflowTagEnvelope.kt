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
package io.infinitic.common.workflows.tags.messages

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.messages.Envelope
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable
import org.apache.avro.Schema

@Serializable
@AvroNamespace("io.infinitic.workflows.tag")
data class WorkflowTagEnvelope(
  private val name: String,
  @AvroNamespace("io.infinitic.workflows.tag") private val type: WorkflowTagMessageType,
  @AvroDefault(Avro.NULL) private val dispatchWorkflowByCustomId: DispatchWorkflowByCustomId? = null,
  private val dispatchMethodByTag: DispatchMethodByTag? = null,
  private val addTagToWorkflow: AddTagToWorkflow? = null,
  private val removeTagFromWorkflow: RemoveTagFromWorkflow? = null,
  private val sendSignalByTag: SendSignalByTag? = null,
  private val cancelWorkflowByTag: CancelWorkflowByTag? = null,
  private val retryWorkflowTaskByTag: RetryWorkflowTaskByTag? = null,
  private val retryTasksByTag: RetryTasksByTag? = null,
  @AvroDefault(Avro.NULL) private val completeTimersByTag: CompleteTimersByTag? = null,
  private val getWorkflowIdsByTag: GetWorkflowIdsByTag? = null
) : Envelope<WorkflowTagMessage> {

  init {
    val noNull = listOfNotNull(
        dispatchWorkflowByCustomId,
        addTagToWorkflow,
        removeTagFromWorkflow,
        sendSignalByTag,
        cancelWorkflowByTag,
        retryWorkflowTaskByTag,
        retryTasksByTag,
        completeTimersByTag,
        dispatchMethodByTag,
        getWorkflowIdsByTag,
    )

    require(noNull.size == 1)
    require(noNull.first() == message())
    require(noNull.first().workflowName.toString() == name)
  }

  companion object {
    fun from(msg: WorkflowTagMessage) = when (msg) {

      is DispatchWorkflowByCustomId -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.DISPATCH_WORKFLOW_BY_CUSTOM_ID,
          dispatchWorkflowByCustomId = msg,
      )

      is AddTagToWorkflow -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.ADD_TAG_TO_WORKFLOW,
          addTagToWorkflow = msg,
      )

      is RemoveTagFromWorkflow -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.REMOVE_TAG_FROM_WORKFLOW,
          removeTagFromWorkflow = msg,
      )

      is SendSignalByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.SEND_SIGNAL_BY_TAG,
          sendSignalByTag = msg,
      )

      is CancelWorkflowByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.CANCEL_WORKFLOW_BY_TAG,
          cancelWorkflowByTag = msg,
      )

      is RetryWorkflowTaskByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.RETRY_WORKFLOW_TASK_BY_TAG,
          retryWorkflowTaskByTag = msg,
      )

      is RetryTasksByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.RETRY_TASKS_BY_TAG,
          retryTasksByTag = msg,
      )

      is CompleteTimersByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.COMPLETE_TIMER_BY_TAG,
          completeTimersByTag = msg,
      )

      is DispatchMethodByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.DISPATCH_METHOD_BY_TAG,
          dispatchMethodByTag = msg,
      )

      is GetWorkflowIdsByTag -> WorkflowTagEnvelope(
          name = "${msg.workflowName}",
          type = WorkflowTagMessageType.GET_WORKFLOW_IDS_BY_TAG,
          getWorkflowIdsByTag = msg,
      )
    }

    /** Deserialize from a byte array and an avro schema */
    fun fromByteArray(bytes: ByteArray, readerSchema: Schema) =
        AvroSerDe.readBinary(bytes, readerSchema, serializer())

    /** Current avro Schema */
    val writerSchema = AvroSerDe.currentSchema(serializer())
  }

  override fun message(): WorkflowTagMessage = when (type) {
    WorkflowTagMessageType.DISPATCH_WORKFLOW_BY_CUSTOM_ID -> dispatchWorkflowByCustomId
    WorkflowTagMessageType.ADD_TAG_TO_WORKFLOW -> addTagToWorkflow
    WorkflowTagMessageType.REMOVE_TAG_FROM_WORKFLOW -> removeTagFromWorkflow
    WorkflowTagMessageType.SEND_SIGNAL_BY_TAG -> sendSignalByTag
    WorkflowTagMessageType.CANCEL_WORKFLOW_BY_TAG -> cancelWorkflowByTag
    WorkflowTagMessageType.RETRY_WORKFLOW_TASK_BY_TAG -> retryWorkflowTaskByTag
    WorkflowTagMessageType.RETRY_TASKS_BY_TAG -> retryTasksByTag
    WorkflowTagMessageType.COMPLETE_TIMER_BY_TAG -> completeTimersByTag
    WorkflowTagMessageType.DISPATCH_METHOD_BY_TAG -> dispatchMethodByTag
    WorkflowTagMessageType.GET_WORKFLOW_IDS_BY_TAG -> getWorkflowIdsByTag
  }!!

  fun toByteArray() = AvroSerDe.writeBinary(this, serializer())
}
