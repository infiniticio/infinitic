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
package io.infinitic.workers.register.config

import io.infinitic.common.workers.config.RetryPolicy
import io.infinitic.events.config.EventListener
import io.infinitic.workflows.WorkflowCheckMode
import io.infinitic.workflows.engine.config.WorkflowStateEngine
import io.infinitic.workflows.tag.config.WorkflowTagEngine

data class WorkflowDefault(
  val concurrency: Int? = null,
  val timeoutInSeconds: Double? = null,
  val retry: RetryPolicy? = null,
  val tagEngine: WorkflowTagEngine? = null,
  var workflowEngine: WorkflowStateEngine? = null,
  val checkMode: WorkflowCheckMode? = null,
  val eventListener: EventListener? = null
) {
  init {
    concurrency?.let {
      require(it >= 0) { error("'${::concurrency.name}' must be positive") }
    }

    timeoutInSeconds?.let {
      require(it > 0) { error("'${::timeoutInSeconds.name}' must be strictly positive") }
    }
  }

  private fun error(msg: String) = "default workflow: $msg"

}
