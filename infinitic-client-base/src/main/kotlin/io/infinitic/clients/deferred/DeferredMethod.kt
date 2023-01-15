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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag

class DeferredMethod<R>(
    internal val returnClass: Class<R>,
    internal val workflowName: WorkflowName,
    internal val methodName: MethodName,
    internal val workflowId: WorkflowId?,
    internal val methodRunId: MethodRunId?,
    internal val workflowTag: WorkflowTag?,
    private val dispatcher: ClientDispatcher
) : Deferred<R> {

  override fun cancelAsync() =
      when {
        workflowId != null ->
            dispatcher.cancelWorkflowAsync(workflowName, workflowId, methodRunId, null)
        workflowTag != null -> dispatcher.cancelWorkflowAsync(workflowName, null, null, workflowTag)
        else -> thisShouldNotHappen()
      }

  // this method retries workflowTask (unique for a workflow instance)
  override fun retryAsync() =
      dispatcher.retryWorkflowTaskAsync(workflowName, workflowId, workflowTag)

  override fun await(): R =
      when {
        workflowId != null ->
            dispatcher.awaitWorkflow(
                returnClass, workflowName, methodName, workflowId, methodRunId!!, true)
        workflowTag != null -> TODO()
        else -> thisShouldNotHappen()
      }

  override val id by lazy {
    when {
      workflowId != null -> methodRunId!!.toString()
      workflowTag != null -> TODO()
      else -> thisShouldNotHappen()
    }
  }

  override val name = workflowName.toString()
}
