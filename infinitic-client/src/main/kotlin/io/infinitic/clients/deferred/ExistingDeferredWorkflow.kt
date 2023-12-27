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
package io.infinitic.clients.deferred

import io.infinitic.clients.Deferred
import io.infinitic.clients.dispatcher.ClientDispatcher
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.proxies.RequestBy
import io.infinitic.common.proxies.RequestByWorkflowId
import io.infinitic.common.proxies.RequestByWorkflowTag
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowName

class ExistingDeferredWorkflow<R> internal constructor(
  internal val workflowName: WorkflowName,
  internal val requestBy: RequestBy,
  internal val methodName: MethodName,
  internal val methodReturnClass: Class<R>,
  internal val methodTimeout: MillisDuration?,
  private val dispatcher: ClientDispatcher
) : Deferred<R> {

  // generate a unique id for this deferred
  val methodRunId = MethodRunId()

  // store time when this deferred was created
  val dispatchTime = System.currentTimeMillis()

  override fun cancelAsync() = dispatcher.cancelWorkflowAsync(workflowName, requestBy, methodRunId)

  // this method retries workflowTask (unique for a workflow instance)
  override fun retryAsync() = dispatcher.retryWorkflowTaskAsync(workflowName, requestBy)

  override fun await(): R = dispatcher.awaitExistingWorkflow(this, true)

  override val id by lazy {
    when (requestBy) {
      is RequestByWorkflowId -> methodRunId.toString()
      is RequestByWorkflowTag -> TODO()
    }
  }

  override val name = workflowName.toString()
}
