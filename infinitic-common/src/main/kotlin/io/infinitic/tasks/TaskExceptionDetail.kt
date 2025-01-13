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
package io.infinitic.tasks

import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.cloudEvents.ERROR_CAUSE
import io.infinitic.cloudEvents.ERROR_DATA
import io.infinitic.cloudEvents.ERROR_MESSAGE
import io.infinitic.cloudEvents.ERROR_NAME
import io.infinitic.cloudEvents.ERROR_STACKTRACE
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.utils.toJson
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlin.reflect.KVisibility
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/** Data class representing an error */
@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
@AvroName("ExceptionDetail")
data class TaskExceptionDetail(
  /** Name of the exception */
  val name: String,

  /** Message of the exception */
  val message: String?,

  /** the stack trace of the exception (String version) */
  val stackTrace: String,

  /** Serialized custom properties of the exception **/
  private val serializedData: Map<String, SerializedData>,

  /** cause of the exception */
  val cause: TaskExceptionDetail?
) : JsonAble {

  /** Mapped custom properties of the exception **/
  val data: Map<String, Any?> by lazy {
    serializedData.mapValues { (_, v) ->
      try {
        v.decode(null, null)
      } catch (e: Exception) {
        "Non deserializable object: ${v.toJson()}"
      }
    }
  }

  companion object {

    fun from(throwable: Throwable): TaskExceptionDetail = TaskExceptionDetail(
        name = throwable::class.java.name,
        message = throwable.message,
        stackTrace = throwable.stackTraceToString(),
        serializedData = captureData(throwable),
        cause = when (val cause = throwable.cause) {
          null, throwable -> null
          else -> from(cause)
        },
    )

    private fun captureData(exception: Throwable): Map<String, SerializedData> =
        exception::class.memberProperties
            .filter {
              // Ensure property is public and not a common property
              (it.visibility == KVisibility.PUBLIC) &&
                  it.name !in listOf(
                  "message",
                  "cause",
                  "stackTrace",
                  "localizedMessage",
                  "suppressed",
              )
            }
            .associateBy(
                { it.name },
                {
                  try {
                    it.isAccessible = true
                    it.getter.call(exception)
                  } catch (e: Exception) {
                    "Error retrieving value: ${e.message}"
                  }
                },
            )
            .mapValues { (_, v) ->
              try {
                SerializedData.encode(v, null, null)
              } catch (e: Exception) {
                SerializedData.encode(
                    "Non serializable object: ${v!!::class.java.name}",
                    null,
                    null,
                )
              }
            }
  }

  // we remove end of line for stackTrace of the output to preserve logs
  override fun toString(): String = this::class.java.simpleName + "(" +
      listOf(
          "name" to name,
          "message" to message,
          "stackTrace" to stackTrace.replace("\n", ""),
          "data" to serializedData.mapValues { (_, v) -> v.toJsonString() },
          "cause" to cause.toString(),
      ).joinToString { "${it.first}=${it.second}" } + ")"

  override fun toJson(): JsonObject = JsonObject(
      mapOf(
          ERROR_NAME to JsonPrimitive(name),
          ERROR_MESSAGE to JsonPrimitive(message),
          ERROR_STACKTRACE to JsonPrimitive(stackTrace),
          ERROR_DATA to JsonObject(serializedData.mapValues { (_, v) -> v.toJson() }),
          ERROR_CAUSE to cause.toJson(),
      ),
  )
}
