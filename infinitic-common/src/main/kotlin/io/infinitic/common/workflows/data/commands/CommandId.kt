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

package io.infinitic.common.workflows.data.commands

import io.infinitic.common.data.Id
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.timers.TimerId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.UUID

@Serializable(with = CommandIdSerializer::class)
data class CommandId(override val id: UUID = UUID.randomUUID()) : Id(id) {
    constructor(taskId: TaskId) : this(taskId.id)
    constructor(timerId: TimerId) : this(timerId.id)
    constructor(workflowId: WorkflowId) : this(workflowId.id)
    constructor(eventId: ChannelEventId) : this(eventId.id)
}

object CommandIdSerializer : KSerializer<CommandId> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("CommandId", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: CommandId) { encoder.encodeString("${value.id}") }
    override fun deserialize(decoder: Decoder) = CommandId(UUID.fromString(decoder.decodeString()))
}
