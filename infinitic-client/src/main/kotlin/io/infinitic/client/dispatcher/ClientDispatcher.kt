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

package io.infinitic.client.dispatcher

import io.infinitic.client.Deferred
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ProxyDispatcher
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.data.TaskSelection
import io.infinitic.common.proxies.data.WorkflowSelection
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import java.util.UUID
import java.util.concurrent.CompletableFuture

interface ClientDispatcher : ProxyDispatcher {

    suspend fun handle(message: ClientMessage)

    override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R =
        dispatch<R>(handler, true, null, null, null).await()

    fun <R : Any?> dispatch(
        handler: ProxyHandler<*>,
        clientWaiting: Boolean,
        tags: Set<String>?,
        options: JobOptions?,
        meta: Map<String, ByteArray>?,
    ): Deferred<R>

    fun <R : Any?> await(task: TaskSelection, clientWaiting: Boolean): R

    fun <R : Any?> await(workflow: WorkflowSelection, clientWaiting: Boolean): R

    fun cancelTask(task: TaskSelection): CompletableFuture<Unit>

    fun cancelWorkflow(workflow: WorkflowSelection): CompletableFuture<Unit>

    fun retryTask(task: TaskSelection): CompletableFuture<Unit>

    fun retryWorkflow(workflow: WorkflowSelection): CompletableFuture<Unit>

    fun getTaskIdsPerTag(taskName: TaskName, taskTag: TaskTag): Set<UUID>

    fun getWorkflowIdsPerTag(workflowName: WorkflowName, workflowTag: WorkflowTag): Set<UUID>
}
