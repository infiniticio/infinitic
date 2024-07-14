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

import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.requester.Requester
import io.infinitic.common.serDe.avro.AvroSerDe
import kotlinx.serialization.Serializable

@Serializable
@AvroNamespace("io.infinitic.tasks")
data class DelegatedTaskData(
  val serviceName: ServiceName,
  val methodName: MethodName,
  val taskId: TaskId,
  val requester: Requester,
  val clientWaiting: Boolean?,
  val taskMeta: TaskMeta,
) {
  fun taskReturnData(returnValue: MethodReturnValue) = TaskReturnValue(
      taskId = taskId,
      serviceName = serviceName,
      methodName = methodName,
      taskMeta = taskMeta,
      returnValue = returnValue,
  )

  fun toByteArray() = AvroSerDe.writeBinaryWithSchemaFingerprint(this, serializer())

  companion object {
    fun fromByteArray(bytes: ByteArray) =
        AvroSerDe.readBinaryWithSchemaFingerprint(bytes, DelegatedTaskData::class)
  }
}
