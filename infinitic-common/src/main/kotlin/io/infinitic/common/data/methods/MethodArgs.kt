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
import io.infinitic.common.utils.getParameterJsonViewClass
import io.infinitic.common.utils.getParameterType
import io.infinitic.exceptions.serialization.ArgumentDeserializationException
import io.infinitic.exceptions.serialization.ArgumentSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import java.lang.reflect.Method

@Serializable(with = MethodParametersSerializer::class)
data class MethodArgs(internal val serializedParameters: List<SerializedData> = listOf()) {

  val size = serializedParameters.size

  fun toJson() = JsonArray(serializedParameters.map { it.toJson() })
}

/**
 * Serializes the provided method parameters.
 */
fun Method.serializeArgs(vararg args: Any?) = MethodArgs(
    args.mapIndexed { index, value ->
      try {
        SerializedData.encode(
            value,
            type = getParameterType(index),
            jsonViewClass = getParameterJsonViewClass(index),
        )
      } catch (e: Exception) {
        throw ArgumentSerializationException(
            declaringClass.name,
            name,
            parameters[index].name,
            parameters[index].parameterizedType.typeName,
            e,
        )
      }
    }.toList(),
)

/**
 * Deserializes the serialized method parameters.
 */
fun Method.deserializeArgs(methodParameters: MethodArgs): Array<*> =
    methodParameters.serializedParameters.mapIndexed { index, serializedData ->
      try {
        serializedData.decode(
            type = getParameterType(index),
            jsonViewClass = getParameterJsonViewClass(index),
        )
      } catch (e: Exception) {
        throw ArgumentDeserializationException(
            declaringClass.name,
            name,
            parameters[index].name,
            getParameterType(index).typeName,
            e,
        )
      }
    }.toTypedArray()

/**
 * Serializer for [MethodArgs] class.
 */
internal object MethodParametersSerializer : KSerializer<MethodArgs> {
  override val descriptor: SerialDescriptor = ListSerializer(SerializedData.serializer()).descriptor

  override fun serialize(encoder: Encoder, value: MethodArgs) {
    ListSerializer(SerializedData.serializer()).serialize(
        encoder,
        value.serializedParameters.toList(),
    )
  }

  override fun deserialize(decoder: Decoder) =
      MethodArgs(ListSerializer(SerializedData.serializer()).deserialize(decoder).toList())
}
