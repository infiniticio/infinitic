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
package io.infinitic.common.tasks.tags.messages

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.MessageId
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.emitters.EmitterName
import io.infinitic.common.messages.Message
import io.infinitic.common.tasks.data.DelegatedTaskData
import io.infinitic.common.tasks.data.ServiceName
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServiceTagMessage : Message {
  val serviceName: ServiceName
  override fun entity() = serviceName.toString()
}

interface WithTagAsKey : ServiceTagMessage {
  val taskTag: TaskTag
  override fun key() = taskTag.toString()
}

interface WithTaskIdAsKey : ServiceTagMessage {
  val taskId: TaskId
  override fun key(): String? = taskId.toString()
}

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
data class SetDelegatedTaskData(
  val delegatedTaskData: DelegatedTaskData,
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val emitterName: EmitterName
) : WithTaskIdAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
data class CompleteDelegatedTask(
  val returnValue: MethodReturnValue,
  override val messageId: MessageId = MessageId(),
  override val serviceName: ServiceName,
  override val taskId: TaskId,
  override val emitterName: EmitterName,
) : WithTaskIdAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
@Deprecated("unused")
data class RetryTaskByTag(
  override val messageId: MessageId = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskTag: TaskTag,
  override val emitterName: EmitterName
) : WithTagAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
data class CancelTaskByTag(
  override val messageId: MessageId = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskTag: TaskTag,
  override val emitterName: EmitterName
) : WithTagAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
@AvroName("AddTagToTask")
data class AddTaskIdToTag(
  val taskId: TaskId,
  override val messageId: MessageId = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskTag: TaskTag,
  override val emitterName: EmitterName
) : WithTagAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
@AvroName("RemoveTagFromTask")
data class RemoveTaskIdFromTag(
  val taskId: TaskId,
  override val messageId: MessageId = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskTag: TaskTag,
  override val emitterName: EmitterName
) : WithTagAsKey

@Serializable
@AvroNamespace("io.infinitic.tasks.tag")
data class GetTaskIdsByTag(
  override val messageId: MessageId = MessageId(),
  @SerialName("taskName") override val serviceName: ServiceName,
  override val taskTag: TaskTag,
  override val emitterName: EmitterName
) : WithTagAsKey
