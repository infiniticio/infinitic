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

package io.infinitic.client

import io.infinitic.client.dispatcher.ClientDispatcher
import io.infinitic.client.dispatcher.ClientDispatcherImpl
import io.infinitic.common.clients.ClientStarter
import io.infinitic.common.clients.Deferred
import io.infinitic.common.clients.InfiniticClient
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.ClientName
import io.infinitic.common.exceptions.thisShouldNotHappen
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.tasks.TaskOptions
import io.infinitic.workflows.DeferredStatus
import io.infinitic.workflows.WorkflowOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class AbstractInfiniticClient : InfiniticClient {
    /**
     * Client's name (must be unique)
     */
    override val name by lazy { clientName.toString() }

    protected abstract val clientName: ClientName

    abstract val clientStarter: ClientStarter

    private val sendToWorkflowTag by lazy { clientStarter.sendToWorkflowTag }
    private val sendToWorkflowEngine by lazy { clientStarter.sendToWorkflowEngine }

    protected val logger = KotlinLogging.logger {}

    /**
     * We use a thread pool that creates new threads as needed, to improve performance when sending messages
     */
    private val sendingThreadPool = Executors.newCachedThreadPool()

    /**
     * Coroutine scope used to send messages
     */
    protected val sendingScope = CoroutineScope(sendingThreadPool.asCoroutineDispatcher() + Job())

    /**
     * Coroutine scope used to listen messages coming back to client
     *
     * We use a different scope for listening than for sending,
     * to be able to wait for all messages to be sent before closing the client
     */
    protected val listeningScope = CoroutineScope(Dispatchers.IO + Job())

    protected val dispatcher: ClientDispatcher by lazy {
        ClientDispatcherImpl(
            sendingScope,
            clientName,
            sendToWorkflowEngine,
            sendToWorkflowTag
        )
    }

    /**
     *  Get last Deferred created by the call of a stub
     */
    override val lastDeferred get() = dispatcher.getLastDeferred()

    /**
     * Close all resources used
     */
    override fun close() {
        // first make sure that all messages are sent
        join()

        // then close everything
        listeningScope.cancel()
        sendingScope.cancel()
        sendingThreadPool.shutdown()
    }

    /**
     * Wait for all messages to be sent
     */
    override fun join() = runBlocking {
        sendingScope.coroutineContext.job.children.forEach { it.join() }
    }

    override suspend fun handle(message: ClientMessage) {
        logger.debug { "Receiving $message" }
        dispatcher.handle(message)
    }

    /**
     *  Create a stub for a new workflow
     */
    override fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String>?,
        options: WorkflowOptions?,
        meta: Map<String, ByteArray>?,
    ): T = NewWorkflowProxyHandler(
        klass = klass,
        workflowTags = tags?.map { WorkflowTag(it) }?.toSet() ?: setOf(),
        workflowOptions = options ?: WorkflowOptions(),
        workflowMeta = WorkflowMeta(meta ?: mapOf())
    ) { dispatcher }.stub()

    /**
     *  Create a stub for an existing workflow targeted by id
     */
    override fun <T : Any> getWorkflowById(
        klass: Class<out T>,
        id: String
    ): T = ExistingWorkflowProxyHandler(
        klass = klass,
        WorkflowId(id),
        null
    ) { dispatcher }.stub()

    /**
     *  Create a stub for existing workflow targeted by tag
     */
    override fun <T : Any> getWorkflowByTag(
        klass: Class<out T>,
        tag: String
    ): T = ExistingWorkflowProxyHandler(
        klass = klass,
        null,
        WorkflowTag(tag)
    ) { dispatcher }.stub()

    /**
     * Await a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> await(
        stub: T
    ): Any? = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler -> when {
            handler.workflowId != null ->
                dispatcher.awaitWorkflow(
                    handler.returnType,
                    handler.workflowName,
                    handler.methodName,
                    handler.workflowId!!,
                    null,
                    false
                )
            handler.workflowTag != null ->
                TODO("Not implemented as tag can target multiple workflows")
            else ->
                thisShouldNotHappen()
        }
        else -> throw InvalidStubException("$stub")
    }

    /**
     * Await a method from a running workflow targeted by its id and the methodRunId
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> await(
        stub: T,
        methodRunId: String
    ): Any? = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler -> when {
            handler.workflowId != null ->
                dispatcher.awaitWorkflow(
                    handler.returnType,
                    handler.workflowName,
                    handler.methodName,
                    handler.workflowId!!,
                    MethodRunId(methodRunId),
                    false
                )
            handler.workflowTag != null ->
                throw InvalidStubException("$stub")
            else ->
                thisShouldNotHappen()
        }
        else -> throw InvalidStubException("$stub")
    }

    /**
     * Cancel a workflow
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> cancelAsync(
        stub: T
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler ->
            dispatcher.cancelWorkflowAsync(handler.workflowName, handler.workflowId, null, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry a workflow task
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> retryWorkflowTaskAsync(
        stub: T,
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler ->
            dispatcher.retryWorkflowTaskAsync(handler.workflowName, handler.workflowId, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry task(s) within workflow(s)
     */
    override fun <T : Any> retryTasksAsync(
        stub: T,
        taskClass: Class<*>?,
        taskStatus: DeferredStatus?,
        taskId: String?,
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler -> {
            val taskName = taskClass?.let {
                // Use NewTaskProxyHandler in case of use of @Name annotation
                NewTaskProxyHandler(it, setOf(), TaskOptions(), TaskMeta()) { dispatcher }.taskName
            }

            dispatcher.retryTaskAsync(
                workflowName = handler.workflowName,
                workflowId = handler.workflowId,
                workflowTag = handler.workflowTag,
                taskName = taskName,
                taskStatus = taskStatus,
                taskId = taskId?.let { TaskId(it) }
            )
        }
        else ->
            throw InvalidStubException("$stub")
    }

    /**
     * get ids of a stub, associated to a specific tag
     */
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> getIds(
        stub: T
    ): Set<String> = when (val handler = getProxyHandler(stub)) {
        is ExistingWorkflowProxyHandler ->
            dispatcher.getWorkflowIdsByTag(handler.workflowName, handler.workflowId, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

    override fun <R> startAsync(
        invoke: () -> R
    ): CompletableFuture<Deferred<R>> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatchAsync(handler)
    }

    override fun startVoidAsync(
        invoke: () -> Unit
    ): CompletableFuture<Deferred<Void>> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatchAsync(handler)
    }

    private fun getProxyHandler(stub: Any): ProxyHandler<*> {
        val exception by lazy { InvalidStubException("$stub") }

        val handler = try {
            Proxy.getInvocationHandler(stub)
        } catch (e: IllegalArgumentException) {
            throw exception
        }

        if (handler !is ProxyHandler<*>) throw exception

        return handler
    }
}
