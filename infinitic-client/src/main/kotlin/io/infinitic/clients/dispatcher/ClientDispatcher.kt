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

package io.infinitic.clients.dispatcher

import io.infinitic.clients.Deferred
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.proxies.ProxyDispatcher
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.workflows.DeferredStatus
import java.util.concurrent.CompletableFuture

interface ClientDispatcher : ProxyDispatcher {

    suspend fun handle(message: ClientMessage)

    fun getLastDeferred(): Deferred<*>?

    fun <R> dispatchAsync(
        handler: ProxyHandler<*>
    ): CompletableFuture<Deferred<R>>

    fun <T> awaitWorkflow(
        returnClass: Class<T>,
        workflowName: WorkflowName,
        methodName: MethodName,
        workflowId: WorkflowId,
        methodRunId: MethodRunId?,
        clientWaiting: Boolean
    ): T

    fun cancelWorkflowAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        methodRunId: MethodRunId?,
        workflowTag: WorkflowTag?
    ): CompletableFuture<Unit>

    fun retryWorkflowTaskAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): CompletableFuture<Unit>

    fun retryTaskAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?,
        taskId: TaskId? = null,
        taskStatus: DeferredStatus? = null,
        taskName: TaskName? = null
    ): CompletableFuture<Unit>

    fun getWorkflowIdsByTag(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): Set<String>
}
