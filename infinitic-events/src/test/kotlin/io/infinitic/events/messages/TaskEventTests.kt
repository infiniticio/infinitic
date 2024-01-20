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
package io.infinitic.events.messages

import com.fasterxml.jackson.module.kotlin.jsonMapper
import io.cloudevents.CloudEvent
import io.cloudevents.jackson.JsonFormat
import io.infinitic.common.data.MillisInstant
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.fixtures.TestFactory
import io.infinitic.common.serDe.SerializedData
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.executors.messages.ExecuteTask
import io.infinitic.events.data.toData
import io.infinitic.events.toCloudEvent
import io.kotest.core.spec.style.StringSpec
import kotlinx.serialization.InternalSerializationApi
import java.time.Instant

fun main() {
  val d = SerializedData.from(12L)
  println(d)

  val json = String(d.bytes)

  println(json)

  val mapper = jsonMapper {}
  val tree = mapper.readTree(json)
  println(tree)


  val executeTask = TestFactory.random<ExecuteTask>().copy(
      methodParameters = MethodParameters(
          listOf(
              null,
              "3",
              Instant.now(),
          ).map { SerializedData.from(it) },
      ),
      taskMeta = TaskMeta(mutableMapOf("1" to "2".toByteArray())),
      taskTags = setOf(TaskTag("tagA"), TaskTag("tagB")),
  )

  println(executeTask.methodParameters)
  println(executeTask.toData())

  val event: CloudEvent = executeTask.toCloudEvent(
      MillisInstant.now(),
      "urn:pulsar:cluster-name/tenant/namespace",
  )

  println(event)

  println(String(JsonFormat().serialize(event)))
}

@OptIn(InternalSerializationApi::class)
class TaskEventTests : StringSpec(
    {

    },
)

