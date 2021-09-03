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

import io.infinitic.client.deferred.DeferredTask
import io.infinitic.client.deferred.DeferredWorkflow
import io.infinitic.client.proxies.ClientDispatcher
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.CancelTaskPerTag
import io.infinitic.common.tasks.tags.messages.RetryTaskPerTag
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskPerTag
import io.infinitic.exceptions.clients.CanNotApplyOnChannelException
import io.infinitic.exceptions.clients.CanNotApplyOnNewTaskStubException
import io.infinitic.exceptions.clients.CanNotApplyOnNewWorkflowStubException
import io.infinitic.exceptions.clients.CanNotAwaitStubPerTag
import io.infinitic.exceptions.clients.CanNotReuseWorkflowStubException
import io.infinitic.exceptions.clients.NotAStubException
import io.infinitic.exceptions.thisShouldNotHappen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.future.future
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.Closeable
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class InfiniticClient : Closeable {
    abstract val clientName: ClientName

    protected abstract val sendToTaskTagEngine: SendToTaskTagEngine
    protected abstract val sendToTaskEngine: SendToTaskEngine
    protected abstract val sendToWorkflowTagEngine: SendToWorkflowTagEngine
    protected abstract val sendToWorkflowEngine: SendToWorkflowEngine

    private val logger = KotlinLogging.logger {}

    private val sendThreadPool = Executors.newCachedThreadPool()

    open val sendingScope = CoroutineScope(sendThreadPool.asCoroutineDispatcher() + Job())

    private val runningThreadPool = Executors.newCachedThreadPool()

    open val runningScope = CoroutineScope(runningThreadPool.asCoroutineDispatcher() + Job())

    private val dispatcher by lazy {
        ClientDispatcher(
            sendingScope,
            clientName,
            sendToTaskTagEngine,
            sendToTaskEngine,
            sendToWorkflowTagEngine,
            sendToWorkflowEngine
        )
    }

    override fun close() {
        // first make sure that all messages are sent
        join()

        // only then, close everything
        sendingScope.cancel()
        sendThreadPool.shutdown()

        runningScope.cancel()
        runningThreadPool.shutdown()
    }

    fun join() = runBlocking {
        sendingScope.coroutineContext.job.children.forEach { it.join() }
    }

    suspend fun handle(message: ClientMessage) {
        logger.debug { "receiving $message" }

        dispatcher.handle(message)
    }

    /**
     * Create stub for a new task
     */
    @JvmOverloads
    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = TaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options ?: TaskOptions(),
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per id
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        id: UUID
    ): T = TaskProxyHandler(
        klass = klass,
        perTaskId = TaskId(id),
        perTag = null
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per tag
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        tag: String
    ): T = TaskProxyHandler(
        klass = klass,
        perTaskId = null,
        perTag = TaskTag(tag)
    ) { dispatcher }.stub()

    /**
     * Synchronous call to get task'ids per tag and name
     */
    fun <T : Any> getTaskIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getTaskIdsPerTag(
        TaskName(klass.name),
        TaskTag(tag)
    )

    /**
     * Create stub for a new workflow
     */
    @JvmOverloads
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = WorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options ?: WorkflowOptions(),
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per id
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        id: UUID
    ): T = WorkflowProxyHandler(
        klass = klass,
        perTag = null,
        perWorkflowId = WorkflowId(id)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per tag
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        tag: String
    ): T = WorkflowProxyHandler(
        klass = klass,
        perTag = WorkflowTag(tag),
        perWorkflowId = null
    ) { dispatcher }.stub()

    /**
     * Synchronous call to get WorkflowIds per tag and name
     */
    fun <T : Any> getWorkflowIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getWorkflowIdsPerTag(
        WorkflowName(klass.name),
        WorkflowTag(tag)
    )

    /**
     *  Asynchronously process a task or a workflow
     */
    fun <T : Any, S> dispatch(proxy: T, method: T.() -> S): Deferred<S> {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "async")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> {
                handler.reset()
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is WorkflowProxyHandler<*> -> {
                if (! handler.isNew()) throw CanNotReuseWorkflowStubException("${handler.workflowName}")
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("cancel")
            else -> thisShouldNotHappen("unknown handle type ${handler::class}")
        }
    }

    /**
     *  Asynchronously process a task (helper)
     */
    @JvmOverloads
    fun <T : Any, S> dispatchTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf(),
        method: T.() -> S
    ) = dispatch(newTask(klass, tags, options, meta), method)

    /**
     *  Asynchronously process a workflow (helper)
     */
    @JvmOverloads
    fun <T : Any, S> dispatchWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf(),
        method: T.() -> S
    ) = dispatch(newWorkflow(klass, tags, options, meta), method)

    /**
     *  Cancel a task or a workflow from a stub
     */
    fun <T : Any> cancel(proxy: T): CompletableFuture<Unit> {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "cancel")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> cancelTaskHandler(handler)
            is WorkflowProxyHandler<*> -> cancelWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("cancel")
            else -> thisShouldNotHappen("Unknown handle type ${handler::class}")
        }
    }

    /**
     *  Cancel a task by id
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getTask(klass, id))

    /**
     *  Cancel a task by tag
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        tag: String
    ) = cancel(getTask(klass, tag))

    /**
     *  Cancel a workflow by id
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getWorkflow(klass, id))

    /**
     *  Cancel a workflow by tag
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        tag: String
    ) = cancel(getWorkflow(klass, tag))

    /**
     * Await a task or a workflowTask from a stub
     */
    fun <T : Any> await(proxy: T): Any {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> awaitTaskHandler(handler)
            is WorkflowProxyHandler<*> -> awaitWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("await")
            else -> thisShouldNotHappen("Unknown handle type ${handler::class}")
        }
    }

    /**
     * Await a task by id
     */
    fun <T : Any> awaitTask(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getTask(klass, id))

    /**
     * Await a workflow by id
     */
    fun <T : Any> awaitWorkflow(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getWorkflow(klass, id))

    /**
     *  Complete a task or a workflow from a stub
     */
    fun <T : Any> complete(proxy: T, value: Any?) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "complete")

        when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> TODO("Not yet implemented")
            is WorkflowProxyHandler<*> -> TODO("Not yet implemented")
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("complete")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     *  Complete a task by id
     */
    fun <T : Any> completeTask(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getTask(klass, id), value)

    /**
     *  Complete a task by tag
     */
    fun <T : Any> completeTask(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getTask(klass, tag), value)

    /**
     *  Complete a workflow by id
     */
    fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getWorkflow(klass, id), value)

    /**
     *  Complete a workflow by tag
     */
    fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getWorkflow(klass, tag), value)

    /**
     * Retry a task or a workflowTask from a stub
     */
    fun <T : Any> retry(proxy: T): CompletableFuture<Unit> {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> retryTaskHandler(handler)
            is WorkflowProxyHandler<*> -> retryWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("retry")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     * Retry a task by id
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        id: UUID
    ) = retry(getTask(klass, id))

    /**
     * Retry a task by tag
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        tag: String
    ) = retry(getTask(klass, tag))

    /**
     * Retry a workflow by id
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = retry(getWorkflow(klass, id))

    /**
     * Retry a workflow by tag
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        tag: String
    ) = retry(getWorkflow(klass, tag))

    private fun <T : Any> cancelTaskHandler(handler: TaskProxyHandler<T>): CompletableFuture<Unit> {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException("${handler.taskName}", "cancel")

        return sendingScope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = CancelTask(
                        taskId = handler.perTaskId!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = CancelTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    private fun <T : Any> retryTaskHandler(handler: TaskProxyHandler<T>): CompletableFuture<Unit> {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException("${handler.taskName}", "retry")

        return sendingScope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = RetryTask(
                        taskId = handler.perTaskId!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = RetryTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    private fun <T : Any> awaitTaskHandler(handler: TaskProxyHandler<T>): Any {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException("${handler.taskName}", "await")

        return when {
            handler.perTaskId != null -> DeferredTask<Any>(
                taskName = handler.taskName,
                taskId = handler.perTaskId!!,
                isSync = false,
                dispatcher = dispatcher
            ).await()
            handler.perTag != null -> throw CanNotAwaitStubPerTag("${handler.taskName}")
            else -> thisShouldNotHappen()
        }
    }

    private fun <T : Any> cancelWorkflowHandler(handler: WorkflowProxyHandler<T>): CompletableFuture<Unit> {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException("${handler.workflowName}", "cancel")

        return sendingScope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = CancelWorkflow(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = handler.workflowName,
                        reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                    )
                    launch { sendToWorkflowEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = CancelWorkflowPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName,
                        reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                    )
                    launch { sendToWorkflowTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    private fun <T : Any> retryWorkflowHandler(handler: WorkflowProxyHandler<T>): CompletableFuture<Unit> {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException("${handler.workflowName}", "retry")

        return sendingScope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = RetryWorkflowTask(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = handler.workflowName
                    )
                    launch { sendToWorkflowEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = RetryWorkflowTaskPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName
                    )
                    launch { sendToWorkflowTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    private fun <T : Any> awaitWorkflowHandler(handler: WorkflowProxyHandler<T>): Any {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException("${handler.workflowName}", "await")

        return when {
            handler.perWorkflowId != null -> DeferredWorkflow<Any>(
                workflowName = handler.workflowName,
                workflowId = handler.perWorkflowId!!,
                isSync = false,
                dispatcher = dispatcher
            ).await()
            handler.perTag != null -> throw CanNotAwaitStubPerTag("${handler.workflowName}")
            else -> thisShouldNotHappen()
        }
    }
}
