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
package io.infinitic.common.data.methods

import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.utils.jsonViewClass
import io.infinitic.exceptions.serialization.ReturnValueDeserializationException
import io.infinitic.exceptions.serialization.ReturnValueSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.lang.reflect.Method
import java.lang.reflect.Type

@Serializable(with = MethodReturnValueSerializer::class)
data class MethodReturnValue(internal val serializedData: SerializedData) {

  companion object {
    fun from(data: Any?, type: Type?) = MethodReturnValue(SerializedData.encode(data, type, null))
  }

  override fun toString() = serializedData.toString()

  fun toJson() = serializedData.toJson()

  fun deserialize(type: Type?, jsonViewClass: Class<out Any>?): Any? =
      serializedData.decode(type, jsonViewClass)
}

/**
 * Serializes the given [MethodReturnValue]
 */
fun Method.encodeReturnValue(value: Any?) = MethodReturnValue(
    try {
      SerializedData.encode(
          value,
          type = genericReturnType,
          jsonViewClass = jsonViewClass,
      )
    } catch (e: Exception) {
      throw ReturnValueSerializationException(declaringClass.name, name, e)
    },
)

/**
 * Deserializes the given [MethodReturnValue]
 */
fun Method.decodeReturnValue(methodReturnValue: MethodReturnValue): Any? = try {
  methodReturnValue.deserialize(
      type = genericReturnType,
      jsonViewClass = jsonViewClass,
  )
} catch (e: Exception) {
  throw ReturnValueDeserializationException(declaringClass.name, name, e)
}

/**
 * Serializer for [MethodReturnValue] class.
 */
internal object MethodReturnValueSerializer : KSerializer<MethodReturnValue> {
  override val descriptor: SerialDescriptor = SerializedData.serializer().descriptor

  override fun serialize(encoder: Encoder, value: MethodReturnValue) {
    SerializedData.serializer().serialize(encoder, value.serializedData)
  }

  override fun deserialize(decoder: Decoder) =
      MethodReturnValue(SerializedData.serializer().deserialize(decoder))
}
