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

import com.github.avrokotlin.avro4k.Avro
import com.github.avrokotlin.avro4k.AvroDefault
import com.github.avrokotlin.avro4k.AvroNamespace
import io.infinitic.cloudEvents.OUTPUT
import io.infinitic.cloudEvents.SERVICE_NAME
import io.infinitic.cloudEvents.TASK_ID
import io.infinitic.cloudEvents.TASK_META
import io.infinitic.cloudEvents.TASK_NAME
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.utils.JsonAble
import io.infinitic.common.utils.toJson
import io.infinitic.common.workflows.data.workflowTasks.isWorkflowTask
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
@AvroNamespace("io.infinitic.tasks.data")
data class TaskReturnValue(
  val taskId: TaskId,
  @SerialName("taskName") val serviceName: ServiceName,
  @AvroDefault(Avro.NULL) val methodName: MethodName? = null,
  val taskMeta: TaskMeta,
  val returnValue: MethodReturnValue
) : JsonAble {
  override fun toJson() = JsonObject(
      when (serviceName.isWorkflowTask()) {
        true -> mapOf(
            OUTPUT to returnValue.toJson(),
            TASK_ID to taskId.toJson(),
        )

        false -> mapOf(
            OUTPUT to returnValue.toJson(),
            TASK_ID to taskId.toJson(),
            TASK_NAME to methodName.toJson(),
            SERVICE_NAME to serviceName.toJson(),
            TASK_META to taskMeta.toJson(),
        )
      },
  )
}
