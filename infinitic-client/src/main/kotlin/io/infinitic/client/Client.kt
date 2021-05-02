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
import io.infinitic.clients.Deferred
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
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import java.lang.reflect.Proxy
import java.util.UUID
import io.infinitic.clients.InfiniticClient as InfiniticClientInterface

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class Client : InfiniticClientInterface {
    abstract val clientName: ClientName
    protected abstract val scope: CoroutineScope
    protected abstract val sendToTaskTagEngine: SendToTaskTagEngine
    protected abstract val sendToTaskEngine: SendToTaskEngine
    protected abstract val sendToWorkflowTagEngine: SendToWorkflowTagEngine
    protected abstract val sendToWorkflowEngine: SendToWorkflowEngine

    private val logger = LoggerFactory.getLogger(javaClass)

    private var _dispatcher: ClientDispatcher? = null

    private val dispatcher: ClientDispatcher
        get() = _dispatcher ?: run {
            _dispatcher = ClientDispatcher(
                scope,
                clientName,
                sendToTaskTagEngine,
                sendToTaskEngine,
                sendToWorkflowTagEngine,
                sendToWorkflowEngine
            )

            _dispatcher!!
        }

    suspend fun handle(message: ClientMessage) {
        logger.warn("receiving {}", message)

        dispatcher.handle(message)
    }

    /**
     * Create stub for a new task
     */
    override fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String>,
        options: TaskOptions?,
        meta: Map<String, ByteArray>
    ): T = TaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options ?: TaskOptions(),
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per id
     */
    override fun <T : Any> getTask(
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
    override fun <T : Any> getTask(
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
    override fun <T : Any> getTaskIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getTaskIdsPerTag(
        TaskName(klass.name),
        TaskTag(tag)
    )

    /**
     * Create stub for a new workflow
     */
    override fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String>,
        options: WorkflowOptions?,
        meta: Map<String, ByteArray>
    ): T = WorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options ?: WorkflowOptions(),
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per id
     */
    override fun <T : Any> getWorkflow(
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
    override fun <T : Any> getWorkflow(
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
    override fun <T : Any> getWorkflowIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getWorkflowIdsPerTag(
        WorkflowName(klass.name),
        WorkflowTag(tag)
    )

    /**
     *  Asynchronously process a task or a workflow
     */
    override fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S> {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "async")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> {
                handler.reset()
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is WorkflowProxyHandler<*> -> {
                if (! handler.isNew()) throw CanNotReuseWorkflowStubException(handler.klass.name)
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("cancel")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     *  Cancel a task or a workflow from a stub
     */
    override fun <T : Any> cancel(proxy: T) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "cancel")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> cancelTaskHandler(handler)
            is WorkflowProxyHandler<*> -> cancelWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("cancel")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     * Await a task or a workflowTask from a stub
     */
    override fun <T : Any> await(proxy: T): Any {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> awaitTaskHandler(handler)
            is WorkflowProxyHandler<*> -> awaitWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("await")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     *  Complete a task or a workflow from a stub
     */
    override fun <T : Any> complete(proxy: T, value: Any) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "complete")

        when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> TODO("Not yet implemented")
            is WorkflowProxyHandler<*> -> TODO("Not yet implemented")
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("complete")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    /**
     * Retry a task or a workflowTask from a stub
     */
    override fun <T : Any> retry(proxy: T) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> retryTaskHandler(handler)
            is WorkflowProxyHandler<*> -> retryWorkflowHandler(handler)
            is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("retry")
            else -> throw RuntimeException("Unknown handle type ${handler::class}")
        }
    }

    private fun <T : Any> cancelTaskHandler(handler: TaskProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException(handler.klass.name, "cancel")

        return scope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = CancelTask(
                        taskId = handler.perTaskId!!,
                        taskName = TaskName(handler.klass.name)
                    )
                    sendToTaskEngine(msg)
                }
                handler.perTag != null -> {
                    val msg = CancelTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = TaskName(handler.klass.name)
                    )
                    sendToTaskTagEngine(msg)
                }
                else -> thisShouldNotHappen()
            }
        }.join()
    }

    private fun <T : Any> retryTaskHandler(handler: TaskProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException(handler.klass.name, "retry")

        return scope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = RetryTask(
                        taskId = handler.perTaskId!!,
                        taskName = TaskName(handler.klass.name)
                    )
                    sendToTaskEngine(msg)
                }
                handler.perTag != null -> {
                    val msg = RetryTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = TaskName(handler.klass.name)
                    )
                    sendToTaskTagEngine(msg)
                }
                else -> thisShouldNotHappen()
            }
        }.join()
    }

    private fun <T : Any> awaitTaskHandler(handler: TaskProxyHandler<T>): Any {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException(handler.klass.name, "await")

        return when {
            handler.perTaskId != null -> DeferredTask<Any>(
                taskName = TaskName(handler.klass.name),
                taskId = handler.perTaskId!!,
                isSync = false,
                dispatcher = dispatcher
            ).await()
            handler.perTag != null -> throw CanNotAwaitStubPerTag(handler.klass.name)
            else -> thisShouldNotHappen()
        }
    }

    private fun <T : Any> cancelWorkflowHandler(handler: WorkflowProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "retry")

        return scope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = CancelWorkflow(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = WorkflowName(handler.klass.name)
                    )
                    sendToWorkflowEngine(msg)
                }
                handler.perTag != null -> {
                    val msg = CancelWorkflowPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = WorkflowName(handler.klass.name)
                    )
                    sendToWorkflowTagEngine(msg)
                }
                else -> thisShouldNotHappen()
            }
        }.join()
    }

    private fun <T : Any> retryWorkflowHandler(handler: WorkflowProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "retry")

        return scope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = RetryWorkflowTask(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = WorkflowName(handler.klass.name)
                    )
                    sendToWorkflowEngine(msg)
                }
                handler.perTag != null -> {
                    val msg = RetryWorkflowTaskPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = WorkflowName(handler.klass.name)
                    )
                    sendToWorkflowTagEngine(msg)
                }
                else -> thisShouldNotHappen()
            }
        }.join()
    }

    private fun <T : Any> awaitWorkflowHandler(handler: WorkflowProxyHandler<T>): Any {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "await")

        return when {
            handler.perWorkflowId != null -> DeferredWorkflow<Any>(
                workflowName = WorkflowName(handler.klass.name),
                workflowId = handler.perWorkflowId!!,
                isSync = false,
                dispatcher = dispatcher
            ).await()
            handler.perTag != null -> throw CanNotAwaitStubPerTag(handler.klass.name)
            else -> thisShouldNotHappen()
        }
    }
}
