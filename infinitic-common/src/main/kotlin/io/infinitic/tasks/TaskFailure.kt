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

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroName
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.cloudEvents.ERROR
import io.infinitic.cloudEvents.PREVIOUS
import io.infinitic.cloudEvents.WORKER_NAME
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.tasks.executors.errors.ExceptionDetail
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.utils.toJson
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/** Data class representing an error */
@Serializable
@AvroNamespace("io.infinitic.tasks.executor")
@AvroName("WorkerError")
data class TaskFailure(
  /** Name of the worker */
  val workerName: String,

  /** Sequence of the retry */
    //@AvroDefault("0") val retrySequence: Int,

  /** Index of the retry */
    //@AvroDefault("0") val retryIndex: Int,

  /** details of the exception */
  @AvroDefault(Avro.NULL) var exceptionDetail: ExceptionDetail?,

  /** cause of the error */
  @SerialName("cause") val previousFailure: TaskFailure?,

  /** Name of the error */
  @Deprecated("Unused after v0.17.0") private val name: String? = null,
  /** Message of the error */
  @Deprecated("Unused after v0.17.0") private val message: String? = null,
  /** String version of the stack trace */
  @Deprecated("Unused after v0.17.0") private val stackTraceToString: String? = null,
) : JsonAble {

  init {
    // Used to convert old data to new format (v0.17.0)
    exceptionDetail = exceptionDetail ?: ExceptionDetail(
        name = name ?: thisShouldNotHappen(),
        message = message,
        stackTrace = stackTraceToString ?: thisShouldNotHappen(),
        serializedData = emptyMap(),
        cause = null,
    )
  }

  override fun toJson(): JsonObject = JsonObject(
      mapOf(
          WORKER_NAME to JsonPrimitive(workerName.toString()),
          ERROR to exceptionDetail.toJson(),
          PREVIOUS to previousFailure.toJson(),
      ),
  )
}
