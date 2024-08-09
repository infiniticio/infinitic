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
package io.infinitic.tests.timers

import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.utils.UtilService
import io.infinitic.workflows.SendChannel
import io.infinitic.workflows.Workflow
import io.infinitic.workflows.or
import java.time.Duration
import java.time.Instant

interface TimerWorkflow {

  val channel: SendChannel<String>

  fun await(millis: Long): Instant

  fun await(instant: Instant): Instant

  fun awaitSignal(millis: Long): String
}

@Suppress("unused")
class TimerWorkflowImpl : Workflow(), TimerWorkflow {

  override val channel = channel<String>()

  private val utilService = newService(
      UtilService::class.java,
      tags = setOf("foo", "bar"),
      meta = mutableMapOf("foo" to "bar".toByteArray()),
  )

  override fun await(millis: Long) = timer(Duration.ofMillis(millis)).await()

  override fun await(instant: Instant) = timer(instant).await()

  override fun awaitSignal(millis: Long): String {
    val deferred = channel.receive()
    val timer = timer(Duration.ofMillis(millis))

    return when (val any = (deferred or timer).await()) {
      is Instant -> "Instant"
      is String -> any
      else -> thisShouldNotHappen()
    }
  }
}
