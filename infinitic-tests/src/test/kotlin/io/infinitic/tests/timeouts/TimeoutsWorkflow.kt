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
package io.infinitic.tests.timeouts

import io.infinitic.annotations.Timeout
import io.infinitic.exceptions.TaskTimedOutException
import io.infinitic.tests.utils.After500MilliSeconds
import io.infinitic.tests.utils.UtilService
import io.infinitic.workflows.Workflow

interface TimeoutsWorkflow {

  // the workflow method 'withMethodTimeout' has a 100ms timeout
  @Timeout(After500MilliSeconds::class)
  fun withMethodTimeout(duration: Long): Long

  fun withTaskTimeout(wait: Long): Long

  fun withCaughtTaskTimeout(wait: Long): Long

  fun withManualRetry(): Int
}

@Suppress("unused")
class TimeoutsWorkflowImpl : Workflow(), TimeoutsWorkflow {

  private val utilService =
      newService(
          UtilService::class.java,
          tags = setOf("foo", "bar"),
          meta = mapOf("foo" to "bar".toByteArray()),
      )
  private val timeoutsWorkflow =
      newWorkflow(TimeoutsWorkflow::class.java, tags = setOf("foo", "bar"))

  override fun withMethodTimeout(duration: Long) = utilService.await(duration)

  // the task 'withTimeout' has a 100ms timeout
  override fun withTaskTimeout(wait: Long): Long = utilService.withTimeout(wait)

  // the task 'withTimeout' has a 100ms timeout
  override fun withCaughtTaskTimeout(wait: Long): Long = try {
    utilService.withTimeout(wait)
  } catch (e: TaskTimedOutException) {
    -1
  }

  // the task 'tryAgain' has a 100ms timeout and wait for 10s for the first sequence
  override fun withManualRetry(): Int = utilService.tryAgain()

}
