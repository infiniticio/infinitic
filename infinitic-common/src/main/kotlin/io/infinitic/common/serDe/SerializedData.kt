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
import com.fasterxml.jackson.databind.JavaType
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.serDe.utils.inferJavaType
import io.infinitic.common.serDe.utils.name
import io.infinitic.common.serDe.utils.toJavaType
import io.infinitic.common.utils.getClass
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskParameters
import io.infinitic.common.workflows.data.workflowTasks.WorkflowTaskReturnValue
import io.infinitic.exceptions.serialization.JsonDeserializationException
import io.infinitic.exceptions.serialization.JsonSerializationException
import io.infinitic.exceptions.serialization.KotlinDeserializationException
import io.infinitic.exceptions.serialization.KotlinSerializationException
import io.infinitic.exceptions.serialization.SerializerNotFoundException
import io.infinitic.serDe.java.Json.mapper
import io.infinitic.serDe.kotlin.json
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.serializerOrNull
import java.lang.reflect.Type
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

@Serializable
@AvroNamespace("io.infinitic.data")
data class SerializedData(
  var bytes: ByteArray,
  @SerialName("type")
  var dataType: SerializedDataType,
  val meta: Map<String, ByteArray> = mapOf()
) {
  companion object {
    // DO NOT CHANGE THOSE VALUES FOR BACKWARD COMPATIBILITY
    private const val WORKFLOW_TASK_PARAMETERS = "WorkflowTaskParameters"
    private const val WORKFLOW_TASK_RETURN_VALUE = "WorkflowTaskReturnValue"
    const val META_JAVA_TYPE = "javaClass"

    private fun String.toBytes(): ByteArray = toByteArray(charset = Charsets.UTF_8)

    @JvmStatic
    val NULL = SerializedData("".toByteArray(), SerializedDataType.NULL, mapOf())

    /** @return serialized value */
    @JvmStatic
    fun <T : Any> encode(
      value: T?,
      type: Type?,
      jsonViewClass: Class<*>?
    ): SerializedData =
        when (value) {
          null -> NULL
          is WorkflowTaskParameters -> encodeWorkflowTaskParameters(value)
          is WorkflowTaskReturnValue -> encodeWorkflowTaskReturnValue(value)
          else -> encodeOther(value, type, jsonViewClass)
        }

    private fun <T : Any> encodeOther(
      value: T,
      type: Type?,
      jsonViewClass: Class<*>?
    ): SerializedData {
      val serializer = type?.let { serializerOrNull(it) }
      return if (serializer == null || jsonViewClass != null) {
        val javaType = type?.let { mapper.constructType(it) }
        encodeJsonWithoutSerializer(value, javaType, jsonViewClass)
      } else {
        @Suppress("UNCHECKED_CAST")
        encodeJsonWithSerializer(value, serializer as KSerializer<T>)
      }
    }

    private fun encodeWorkflowTaskParameters(value: WorkflowTaskParameters) = SerializedData(
        dataType = SerializedDataType.AVRO_WITH_SCHEMA,
        bytes = value.toByteArray(),
        meta = mapOf(META_JAVA_TYPE to WORKFLOW_TASK_PARAMETERS.toBytes()),
    )

    private fun encodeWorkflowTaskReturnValue(value: WorkflowTaskReturnValue) = SerializedData(
        dataType = SerializedDataType.AVRO_WITH_SCHEMA,
        bytes = value.toByteArray(),
        meta = mapOf(META_JAVA_TYPE to WORKFLOW_TASK_RETURN_VALUE.toBytes()),
    )

    private fun <T : Any> encodeJsonWithSerializer(
      value: T,
      serializer: KSerializer<T>,
    ): SerializedData {
      val jsonString = try {
        json.encodeToString(serializer, value)
      } catch (e: Exception) {
        throw KotlinSerializationException(value, e)
      }
      return SerializedData(
          dataType = SerializedDataType.JSON,
          bytes = jsonString.toByteArray(),
          meta = mapOf(),
      )
    }

    private fun encodeJsonWithoutSerializer(
      value: Any,
      javaType: JavaType?,
      jsonViewClass: Class<*>?
    ): SerializedData {
      // if type is unknown (it should be the case for inline tasks only)
      // we store the type inferred from the provided value
      val inferType by lazy { value.inferJavaType() }
      val (meta, actualType) = when (javaType) {
        null -> mapOf(META_JAVA_TYPE to inferType.name().toBytes()) to inferType
        else -> mapOf<String, ByteArray>() to javaType
      }
      val jsonString = try {
        mapper.writerFor(actualType)
            .let { if (jsonViewClass != null) it.withView(jsonViewClass) else it }
            .writeValueAsString(value)
      } catch (e: Exception) {
        throw JsonSerializationException(value, actualType.name(), e)
      }
      return SerializedData(
          bytes = jsonString.toByteArray(),
          dataType = SerializedDataType.JSON,
          meta = meta,
      )
    }
  }

  /** @return deserialized value */
  fun decode(type: Type?, jsonViewClass: Class<*>?): Any? =
      when (dataType) {
        SerializedDataType.NULL -> null
        SerializedDataType.AVRO_WITH_SCHEMA -> decodeAvroWithSchema()
        SerializedDataType.JSON -> decodeJson(type, jsonViewClass)
        SerializedDataType.JSON_KOTLIN -> decodeJsonKotlin()
        SerializedDataType.JSON_JACKSON -> decodeJsonJackson(jsonViewClass)
      }

  fun toJson() = json.parseToJsonElement(toJsonString())

  fun toJsonString(): String = when (dataType) {
    SerializedDataType.NULL -> "null"
    SerializedDataType.JSON,
    SerializedDataType.JSON_JACKSON,
    SerializedDataType.JSON_KOTLIN -> String(bytes, Charsets.UTF_8)

    SerializedDataType.AVRO_WITH_SCHEMA -> JsonPrimitive(
        Base64.getEncoder().encodeToString(bytes),
    ).toString()
  }

  fun hash(): String {
    // MD5 implementation, enough to avoid collision in practical cases
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(bytes)).toString(16).padStart(32, '0')
  }

  /** Readable version */
  override fun toString() = mapOf(
      "bytes" to toJsonString().replace("\n", ""),
      "type" to dataType,
      "meta" to meta.mapValues { String(it.value) },
  ).toString()

  private fun decodeAvroWithSchema(): Any {
    return when (getMetaJavaTypeString()) {
      WORKFLOW_TASK_PARAMETERS -> WorkflowTaskParameters.fromByteArray(bytes)
      WORKFLOW_TASK_RETURN_VALUE -> WorkflowTaskReturnValue.fromByteArray(bytes)
      else -> thisShouldNotHappen()
    }
  }

  private fun decodeJson(type: Type?, jsonViewClass: Class<*>?): Any {
    val serializer = type?.let { serializerOrNull(it) }
    val javaType = type?.let { mapper.constructType(it) }
      ?: getMetaJavaTypeString()?.toJavaType() ?: thisShouldNotHappen()
    return if (type == null || serializer == null || jsonViewClass != null) {
      decodeJsonWithoutSerializer(javaType, jsonViewClass)
    } else {
      decodeJsonWithSerializer(serializer, javaType)
    }
  }

  private fun decodeJsonWithoutSerializer(
    javaType: JavaType,
    jsonViewClass: Class<*>?
  ): Any {
    val jsonStr = toJsonString()
    return try {
      mapper.readerFor(javaType)
          .let { if (jsonViewClass != null) it.withView(jsonViewClass) else it }
          .readValue(jsonStr) as Any
    } catch (e: Exception) {
      throw JsonDeserializationException(jsonStr, javaType.name(), e)
    }
  }

  private fun decodeJsonWithSerializer(
    serializer: KSerializer<*>,
    javaType: JavaType
  ): Any {
    val jsonStr = toJsonString()
    return try {
      json.decodeFromString(serializer, jsonStr) as Any
    } catch (e: Exception) {
      throw KotlinDeserializationException(jsonStr, javaType.name(), e)
    }
  }

  @Deprecated("JSON_Jackson should not be used anymore after version 0.15.0")
  private fun decodeJsonJackson(jsonViewClass: Class<*>?): Any {
    val klass = getMetaJavaTypeString()!!.getClass().getOrThrow()
    val jsonStr = toJsonString()
    return try {
      when (jsonViewClass) {
        null -> mapper.reader()
        else -> mapper.readerWithView(jsonViewClass)
      }.readValue(jsonStr, klass)
    } catch (e: JsonProcessingException) {
      throw JsonDeserializationException(jsonStr, klass.name, e)
    }
  }

  @Deprecated("JSON_KOTLIN should not be used anymore after version 0.15.0")
  private fun decodeJsonKotlin() {
    val klass = getMetaJavaTypeString()!!.getClass().getOrThrow()

    @OptIn(InternalSerializationApi::class)
    val serializer = klass.kotlin.serializerOrNull()
      ?: throw SerializerNotFoundException(klass.name)
    val jsonStr = toJsonString()
    try {
      json.decodeFromString(serializer, jsonStr)
    } catch (e: SerializationException) {
      throw KotlinDeserializationException(jsonStr, klass.name, e)
    }
  }

  private fun getMetaJavaTypeString(): String? = meta[META_JAVA_TYPE]?.let {
    String(it, charset = Charsets.UTF_8)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as SerializedData

    if (!bytes.contentEquals(other.bytes)) return false
    if (dataType != other.dataType) return false

    if (meta.keys != other.meta.keys) return false
    if (meta.map { it.value.contentEquals(other.meta[it.key]!!) }.any { !it }) return false

    return true
  }

  override fun hashCode(): Int = bytes.contentHashCode()
}
