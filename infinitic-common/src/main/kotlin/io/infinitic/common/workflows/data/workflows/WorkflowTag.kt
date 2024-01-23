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
package io.infinitic.common.workflows.data.workflows

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = WorkflowTagSerializer::class)
data class WorkflowTag(val tag: String) {
  override fun toString() = tag

  companion object {
    const val CUSTOM_ID_PREFIX = "customId:"
  }

  fun isCustomId() = tag.startsWith(CUSTOM_ID_PREFIX)
}

object WorkflowTagSerializer : KSerializer<WorkflowTag> {
  override val descriptor: SerialDescriptor =
      PrimitiveSerialDescriptor("WorkflowTag", PrimitiveKind.STRING)

  override fun serialize(encoder: Encoder, value: WorkflowTag) {
    encoder.encodeString(value.tag)
  }

  override fun deserialize(decoder: Decoder) = WorkflowTag(decoder.decodeString())
}

val Set<WorkflowTag>.set get() = map { it.tag }.toSet()
