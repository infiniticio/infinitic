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

package io.infinitic.client.deferred

import io.infinitic.client.Deferred
import io.infinitic.client.proxies.ClientDispatcher
import io.infinitic.common.proxies.RunningWorkflowProxyHandler
import io.infinitic.exceptions.thisShouldNotHappen
import java.util.UUID
import java.util.concurrent.CompletableFuture

internal class DeferredWorkflow<R : Any?> (
    private val runningWorkflowProxyHandler: RunningWorkflowProxyHandler<*>,
    internal val isSync: Boolean,
    private val dispatcher: ClientDispatcher,
    private val future: CompletableFuture<Unit>? = null
) : Deferred<R> {

    override fun cancel() = dispatcher.cancelWorkflow(runningWorkflowProxyHandler)

    override fun retry() = dispatcher.retryWorkflow(runningWorkflowProxyHandler)

    val workflowName = runningWorkflowProxyHandler.workflowName
    val workflowId = runningWorkflowProxyHandler.perWorkflowId!!

    override fun await(): R = dispatcher.await(this)

    override fun join(): Deferred<R> {
        when (future) {
            null -> thisShouldNotHappen()
            else -> future.join()
        }

        return this
    }

    override val id: UUID = workflowId.id

    @Suppress("UNCHECKED_CAST")
    override fun <K : Any> instance() = runningWorkflowProxyHandler.stub() as K
}
