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

import com.fasterxml.jackson.annotation.JsonView
import com.fasterxml.jackson.core.JsonProcessingException
import io.infinitic.common.serDe.SerializedData
import io.infinitic.exceptions.serialization.ParameterSerializationException
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import org.jetbrains.annotations.TestOnly
import java.lang.reflect.Method
import java.lang.reflect.Parameter
import java.security.InvalidParameterException
import kotlin.reflect.KClass

@Serializable(with = MethodParametersSerializer::class)
data class MethodParameters(val parameters: List<SerializedData> = listOf()) :
  Collection<SerializedData> by parameters {

  fun toJson() = JsonArray(parameters.map { it.toJson() })

  companion object {
    fun from(method: Method, data: Array<*>): MethodParameters {
      val parameters = method.parameters

      return MethodParameters(
          data.mapIndexed { index, value ->
            try {
              SerializedData.from(value, jsonViewClass = parameters[index].getJsonViewClass())
            } catch (e: JsonProcessingException) {
              throw ParameterSerializationException(
                  method.parameters[index].name,
                  method.parameterTypes[index].kotlin.javaObjectType.name,
                  method.name,
                  method.declaringClass.name,
              )
            }
          }.toList(),
      )
    }

    @TestOnly
    fun from(vararg data: Any?) = MethodParameters(data.map { SerializedData.from(it) }.toList())
  }
}

object MethodParametersSerializer : KSerializer<MethodParameters> {
  override val descriptor: SerialDescriptor = ListSerializer(SerializedData.serializer()).descriptor

  override fun serialize(encoder: Encoder, value: MethodParameters) {
    ListSerializer(SerializedData.serializer()).serialize(encoder, value.parameters.toList())
  }

  override fun deserialize(decoder: Decoder) =
      MethodParameters(ListSerializer(SerializedData.serializer()).deserialize(decoder).toList())
}

private fun Method.getJsonViewClasses(): Array<KClass<*>>? {
  val jsonViewAnnotation = getAnnotation(JsonView::class.java)
  jsonViewAnnotation?.let { annotation ->
    annotation.value.let { if (it.isNotEmpty()) return it }
  }

  return null
}

private fun Parameter.getJsonViewClass(): Class<*>? {
  val jsonViewAnnotation = getAnnotation(JsonView::class.java)
  jsonViewAnnotation?.value?.let {
    when {
      it.size == 1 -> return it[0].java
      else -> throw InvalidParameterException("When present the annotation @JsonView must have one parameter")
    }
  }

  return null
}
