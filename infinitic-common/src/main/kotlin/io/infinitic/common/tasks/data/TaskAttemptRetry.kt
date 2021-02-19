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

package io.infinitic.common.tasks.data

import io.infinitic.common.data.IntInterface
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = TaskAttemptRetrySerializer::class)
data class TaskAttemptRetry(override var int: kotlin.Int = 0) : IntInterface

object TaskAttemptRetrySerializer : KSerializer<TaskAttemptRetry> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("TaskAttemptRetry", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: TaskAttemptRetry) { encoder.encodeInt(value.int) }
    override fun deserialize(decoder: Decoder) = TaskAttemptRetry(decoder.decodeInt())
}
