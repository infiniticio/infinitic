/**
 * "Commons Clause" License Condition v1.0
 *
 * The Software is provided to you by the Licensor under the License, as defined
 * below, subject to the following condition.
 *
 * Without limiting other conditions in the License, the grant of rights under the
 * License will not include, and the License does not grant to you, the right to
 * Sell the Software.
 *
 * For purposes of the foregoing, “Sell” means practicing any or all of the rights
 * granted to you under the License to provide to third parties, for a fee or
 * other consideration (including without limitation fees for hosting or
 * consulting/ support services related to the Software), a product or service
 * whose value derives, entirely or substantially, from the functionality of the
 * Software. Any license notice or attribution required by the License must also
 * include this Commons Clause License Condition notice.
 *
 * Software: Infinitic
 *
 * License: MIT License (https://opensource.org/licenses/MIT)
 *
 * Licensor: infinitic.io
 */

package io.infinitic.client.proxies

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.proxies.MethodProxyHandler
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future

internal class ExistingWorkflowProxyHandler<T : Any>(
    private val klass: Class<T>,
    private val id: String,
    private val workflowOptions: WorkflowOptions?,
    private val workflowMeta: WorkflowMeta?,
    private val clientOutput: ClientOutput
) : MethodProxyHandler<T>(klass) {

    /*
     * Cancel a workflow
     */
    fun cancelWorkflowAsync(output: Any?) {
        val msg = CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowOutput = MethodOutput.from(output)
        )
        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, 0F) }.join()

        // allow stub reuse
        reset()
    }

    /*
     * Retry a task
     */
    fun retryWorkflowTask() {
        if (method != null) { throw RuntimeException("Do not provide a method when retrying a workflowTask") }
        TODO()
//        val msg = RetryWorkflowTask(
//            workflowId = Workflow(id),
//            workflowName = TaskName(klass.name),
//            workflowOptions = workflowOptions,
//            workflowMeta = workflowMeta
//        )
//        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, 0F) }.join()

        // reset method to allow reuse of the stub
        reset()
    }
}
