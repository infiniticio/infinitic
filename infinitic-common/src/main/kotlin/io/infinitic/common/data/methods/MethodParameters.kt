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

import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.utils.getJsonViewClassOnParameter
import io.infinitic.exceptions.serialization.ParameterSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import java.lang.reflect.Method

@Serializable(with = MethodParametersSerializer::class)
data class MethodParameters(internal val serializedParameters: List<SerializedData> = listOf()) {

  val size = serializedParameters.size

  fun toJson() = JsonArray(serializedParameters.map { it.toJson() })

  companion object {
    fun from(vararg data: Any?) =
        MethodParameters(data.map { SerializedData.from(it) }.toList())
  }
}

fun Method.serialize(data: Array<*>) = MethodParameters(
    data.mapIndexed { index, value ->
      val jsonViewClass = getJsonViewClassOnParameter(index)
      try {
        when(jsonViewClass) {
          null -> SerializedData.from(value)
          else ->  SerializedData.from(value, jsonViewClass)
        }
        SerializedData.from(value, jsonViewClass = jsonViewClass)
      } catch (e: JsonProcessingException) {
        throw ParameterSerializationException(
            parameters[index].name,
            parameterTypes[index].kotlin.javaObjectType.name,
            name,
            declaringClass.name,
        )
      }
    }.toList()
)

fun Method.deserialize(methodParameters: MethodParameters): Array<*> =
    methodParameters.serializedParameters.mapIndexed { index, serializedData ->
      serializedData.deserialize(getJsonViewClassOnParameter(index))
    }.toTypedArray()

internal object MethodParametersSerializer : KSerializer<MethodParameters> {
  override val descriptor: SerialDescriptor = ListSerializer(SerializedData.serializer()).descriptor

  override fun serialize(encoder: Encoder, value: MethodParameters) {
    ListSerializer(SerializedData.serializer()).serialize(
        encoder,
        value.serializedParameters.toList(),
    )
  }

  override fun deserialize(decoder: Decoder) =
      MethodParameters(ListSerializer(SerializedData.serializer()).deserialize(decoder).toList())
}
