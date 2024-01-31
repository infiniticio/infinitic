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
package io.infinitic.common.tasks.data

import io.infinitic.common.utils.JsonAble
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.util.*

@Serializable(with = TaskMetaSerializer::class)
data class TaskMeta(val map: MutableMap<String, ByteArray> = mutableMapOf()) :
  MutableMap<String, ByteArray> by map, JsonAble {
  override fun toJson() = JsonObject(
      map.mapValues { JsonPrimitive(Base64.getEncoder().encodeToString(it.value)) },
  )

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as TaskMeta
    if (map.keys != other.map.keys) return false
    if (map.map { it.value.contentEquals(other.map[it.key]!!) }.any { !it }) return false

    return true
  }

  override fun hashCode() = map.hashCode()
}

object TaskMetaSerializer : KSerializer<TaskMeta> {
  private val ser = MapSerializer(String.serializer(), ByteArraySerializer())
  override val descriptor: SerialDescriptor = ser.descriptor

  override fun serialize(encoder: Encoder, value: TaskMeta) {
    ser.serialize(encoder, value.map)
  }

  override fun deserialize(decoder: Decoder) = TaskMeta(ser.deserialize(decoder).toMutableMap())
}
