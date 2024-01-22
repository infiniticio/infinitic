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
package io.infinitic.common.serDe

import com.fasterxml.jackson.core.JsonProcessingException
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.utils.getClass
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.exceptions.serialization.JsonDeserializationException
import io.infinitic.exceptions.serialization.KotlinDeserializationException
import io.infinitic.exceptions.serialization.MissingMetaJavaClassException
import io.infinitic.exceptions.serialization.SerializerNotFoundException
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*
import io.infinitic.common.serDe.json.Json as JsonJackson

@Serializable
@AvroNamespace("io.infinitic.data")
data class SerializedData(
  var bytes: ByteArray,
  @AvroNamespace("io.infinitic.data") var type: SerializedDataType,
  val meta: Map<String, ByteArray> = mapOf()
) {
  companion object {
    // DO NOT CHANGE THOSE VALUES
    private const val WORKFLOW_TASK_PARAMETERS = "WorkflowTaskParameters"
    private const val WORKFLOW_TASK_RETURN_VALUE = "WorkflowTaskReturnValue"

    // meta key containing the name of the serialized java class
    const val META_JAVA_CLASS = "javaClass"

    // use a less obvious key than "type" for polymorphic data, to avoid collusion
    private val jsonKotlin = Json {
      classDiscriminator = "#klass"
      ignoreUnknownKeys = true
    }

    private fun String.toBytes(): ByteArray = toByteArray(charset = Charsets.UTF_8)

    private fun Any.getClassInBytes(): ByteArray = this::class.java.name.toBytes()

    /** @return serialized value */
    fun <T : Any> from(value: T?): SerializedData {
      val bytes: ByteArray
      val type: SerializedDataType
      val meta: Map<String, ByteArray>

      when (value) {
        null -> {
          type = SerializedDataType.NULL
          bytes = "".toByteArray()
          meta = mutableMapOf()
        }

        is WorkflowTaskParameters -> {
          type = SerializedDataType.AVRO_WITH_SCHEMA
          bytes = value.toByteArray()
          meta = mutableMapOf(META_JAVA_CLASS to WORKFLOW_TASK_PARAMETERS.toBytes())
        }

        is WorkflowTaskReturnValue -> {
          type = SerializedDataType.AVRO_WITH_SCHEMA
          bytes = value.toByteArray()
          meta = mutableMapOf(META_JAVA_CLASS to WORKFLOW_TASK_RETURN_VALUE.toBytes())
        }

        else -> {
          @OptIn(InternalSerializationApi::class)
          when (val serializer = value::class.serializerOrNull()) {
            null -> {
              type = SerializedDataType.JSON_JACKSON
              bytes = JsonJackson.stringify(value).toByteArray()
            }

            else -> {
              type = SerializedDataType.JSON_KOTLIN
              @Suppress("UNCHECKED_CAST")
              bytes = jsonKotlin.encodeToString(serializer as KSerializer<T>, value).toByteArray()
            }
          }
          meta = mutableMapOf(META_JAVA_CLASS to value.getClassInBytes())
        }
      }
      return SerializedData(bytes, type, meta)
    }
  }

  /** @return deserialized value */
  fun deserialize(): Any? =
      when (type) {
        SerializedDataType.NULL -> null

        SerializedDataType.JSON_JACKSON -> {
          val klass = getDataClass()
          try {
            JsonJackson.parse(toJsonString(), klass)
          } catch (e: JsonProcessingException) {
            throw JsonDeserializationException(klass.name, causeString = e.toString())
          }
        }

        SerializedDataType.JSON_KOTLIN -> {
          val klass = getDataClass()

          @OptIn(InternalSerializationApi::class)
          val serializer = klass.kotlin.serializerOrNull()
            ?: throw SerializerNotFoundException(klass.name)

          try {
            jsonKotlin.decodeFromString(serializer, toJsonString())
          } catch (e: SerializationException) {
            throw KotlinDeserializationException(klass.name, causeString = e.toString())
          }
        }

        SerializedDataType.AVRO_WITH_SCHEMA -> {
          when (getDataClassString()) {
            WORKFLOW_TASK_PARAMETERS -> WorkflowTaskParameters.fromByteArray(bytes)
            WORKFLOW_TASK_RETURN_VALUE -> WorkflowTaskReturnValue.fromByteArray(bytes)
            else -> thisShouldNotHappen()
          }
        }
      }

  fun toJson() = Json.parseToJsonElement(toJsonString())

  fun toJsonString(): String = when (type) {
    SerializedDataType.NULL -> "null"
    SerializedDataType.JSON_JACKSON, SerializedDataType.JSON_KOTLIN -> String(bytes, Charsets.UTF_8)
    SerializedDataType.AVRO_WITH_SCHEMA -> JsonPrimitive(
        Base64.getEncoder().encodeToString(bytes),
    ).toString()
//    SerializedDataType.AVRO_WITH_SCHEMA -> when (getDataClassString()) {
//      WORKFLOW_TASK_PARAMETERS ->
//        JsonJackson.stringify(WorkflowTaskParameters.fromByteArray(bytes))
//
//      WORKFLOW_TASK_RETURN_VALUE ->
//        JsonJackson.stringify(WorkflowTaskReturnValue.fromByteArray(bytes))
//
//      else -> thisShouldNotHappen()
//    }
  }

  fun hash(): String {
    // MD5 implementation, enough to avoid collision in practical cases
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
  }

  /** Readable version */
  override fun toString() = mapOf(
      "bytes" to toJsonString().replace("\n", ""),
      "type" to type,
      "meta" to meta.mapValues { String(it.value) },
  ).toString()

  private fun getDataClass(): Class<out Any> = getDataClassName().getClass().getOrThrow()

  private fun getDataClassName(): String = when (val klass = getDataClassString()) {
    WORKFLOW_TASK_PARAMETERS -> WorkflowTaskParameters::class.java.name
    WORKFLOW_TASK_RETURN_VALUE -> WorkflowTaskReturnValue::class.java.name
    null -> throw MissingMetaJavaClassException
    else -> klass
  }

  private fun getDataClassString(): String? =
      meta[META_JAVA_CLASS]?.let { String(it, charset = Charsets.UTF_8) }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SerializedData

    if (!bytes.contentEquals(other.bytes)) return false
    if (type != other.type) return false

    if (meta.keys != other.meta.keys) return false
    if (meta.map { it.value.contentEquals(other.meta[it.key]!!) }.any { !it }) return false

    return true
  }

  override fun hashCode(): Int = bytes.contentHashCode()
}
