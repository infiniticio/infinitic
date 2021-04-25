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

import io.infinitic.client.deferred.Deferred
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
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.exceptions.clients.CanNotApplyOnNewTaskStubException
import io.infinitic.exceptions.clients.CanNotApplyOnNewWorkflowStubException
import io.infinitic.exceptions.clients.CanNotReuseWorkflowStubException
import io.infinitic.exceptions.clients.IncorrectExistingStubException
import io.infinitic.exceptions.clients.NotAStubException
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import org.slf4j.LoggerFactory
import java.lang.reflect.Proxy
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class Client {
    abstract val clientName: ClientName
    protected abstract val sendToTaskTagEngine: SendToTaskTagEngine
    protected abstract val sendToTaskEngine: SendToTaskEngine
    protected abstract val sendToWorkflowTagEngine: SendToWorkflowTagEngine
    protected abstract val sendToWorkflowEngine: SendToWorkflowEngine

    abstract fun close()

    private val logger = LoggerFactory.getLogger(javaClass)

    private var _dispatcher: ClientDispatcher? = null

    private val dispatcher: ClientDispatcher
        get() = _dispatcher ?: run {
            _dispatcher = ClientDispatcher(
                clientName,
                sendToTaskTagEngine,
                sendToTaskEngine,
                sendToWorkflowTagEngine,
                sendToWorkflowEngine
            )

            _dispatcher!!
        }

    suspend fun handle(message: ClientMessage) {
        logger.debug("receiving {}", message)

        dispatcher.handle(message)
    }

    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> newTask(
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

    /*
     * (Kotlin) Create stub for a new task
     */
    inline fun <reified T : Any> newTask(
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = newTask(T::class.java, tags, options, meta)

    /*
     * Create stub for an existing task targeted per id
     */
    @JvmOverloads fun <T : Any> getTask(
        klass: Class<out T>,
        id: UUID,
        tags: Set<String>? = null,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = TaskProxyHandler(
        klass = klass,
        taskTags = tags?.map { TaskTag(it) }?.toSet(),
        taskOptions = options,
        taskMeta = meta?.let { TaskMeta(it) },
        perTaskId = TaskId(id),
        perTag = null
    ) { dispatcher }.stub()

    /*
     * Create stub for an existing task targeted per tag
     */
    @JvmOverloads fun <T : Any> getTask(
        klass: Class<out T>,
        tag: String,
        tags: Set<String>? = null,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = TaskProxyHandler(
        klass = klass,
        taskTags = tags?.map { TaskTag(it) }?.toSet(),
        taskOptions = options,
        taskMeta = meta?.let { TaskMeta(it) },
        perTaskId = null,
        perTag = TaskTag(tag)
    ) { dispatcher }.stub()

    /*
     * (Kotlin) Create stub for an existing task targeted per id
     */
    inline fun <reified T : Any> getTask(
        id: UUID,
        tags: Set<String>? = null,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = getTask(T::class.java, id, tags, options, meta)

    /*
     * (Kotlin) Create stub for an existing task targeted per tag
     */
    inline fun <reified T : Any> getTask(
        tag: String,
        tags: Set<String>? = null,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = getTask(T::class.java, tag, tags, options, meta)

    /*
     * Create stub for a new workflow
     */
    @JvmOverloads fun <T : Any> newWorkflow(
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

    /*
     * (Kotlin) Create stub for a new workflow
     */
    inline fun <reified T : Any> newWorkflow(
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = newWorkflow(T::class.java, tags, options, meta)

    /*
     * Create stub for an existing workflow per id
     */
    @JvmOverloads fun <T : Any> getWorkflow(
        klass: Class<out T>,
        id: UUID,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = WorkflowProxyHandler(
        klass = klass,
        perTag = null,
        perWorkflowId = WorkflowId(id),
        workflowTags = tags?.map { WorkflowTag(it) }?.toSet(),
        workflowOptions = options,
        workflowMeta = meta?.let { WorkflowMeta(it) }
    ) { dispatcher }.stub()

    /*
     * Create stub for an existing workflow per tag
     */
    @JvmOverloads fun <T : Any> getWorkflow(
        klass: Class<out T>,
        tag: String,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T = WorkflowProxyHandler(
        klass = klass,
        perTag = WorkflowTag(tag),
        perWorkflowId = null,
        workflowTags = tags?.map { WorkflowTag(it) }?.toSet(),
        workflowOptions = options,
        workflowMeta = meta?.let { WorkflowMeta(it) }
    ) { dispatcher }.stub()

    /*
     * (kotlin) Create stub for an existing workflow per id
     */
    inline fun <reified T : Any> getWorkflow(
        id: UUID,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = getWorkflow(T::class.java, id, tags, options, meta)

    /*
     * (kotlin) Create stub for an existing workflow per tag
     */
    inline fun <reified T : Any> getWorkflow(
        tag: String,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = getWorkflow(T::class.java, tag, tags, options, meta)

    /*
     *  Asynchronously process a task or a workflow
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S> {
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
            else -> throw RuntimeException()
        }
    }

    /*
     *  Cancel a task or a workflow
     */
    fun <T : Any> cancel(proxy: T) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "cancel")

        when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> cancelTask(handler)
            is WorkflowProxyHandler<*> -> cancelWorkflow(handler)
            is SendChannelProxyHandler<*> -> throw IncorrectExistingStubException(handler.klass.name, "cancel")
            else -> throw RuntimeException()
        }
    }

    /*
     * Retry a task or a workflowTask
     * when a non-null parameter is provided, it will supersede current one
     */
    fun <T : Any> retry(proxy: T) {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> retryTask(handler)
            is WorkflowProxyHandler<*> -> retryWorkflow(handler)
            is SendChannelProxyHandler<*> -> throw IncorrectExistingStubException(handler.klass.name, "retry")
            else -> throw RuntimeException()
        }
    }

    private fun <T : Any> cancelTask(handler: TaskProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException(handler.klass.name, "cancel")

        when (handler.perTaskId) {
            null -> {
                val msg = CancelTaskPerTag(
                    taskTag = handler.perTag!!,
                    taskName = TaskName(handler.klass.name)
                )
                GlobalScope.future { sendToTaskTagEngine(msg) }.join()
            }
            else -> {
                val msg = CancelTask(
                    taskId = handler.perTaskId!!,
                    taskName = TaskName(handler.klass.name)
                )
                GlobalScope.future { sendToTaskEngine(msg) }.join()
            }
        }
    }

    private fun <T : Any> retryTask(handler: TaskProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewTaskStubException(handler.klass.name, "retry")

        if (handler.perTaskId != null) {
            val msg = RetryTask(
                taskId = handler.perTaskId!!,
                taskName = TaskName(handler.klass.name),
                methodName = null,
                methodParameterTypes = null,
                methodParameters = null,
                taskTags = handler.taskTags,
                taskOptions = handler.taskOptions,
                taskMeta = handler.taskMeta
            )
            GlobalScope.future { sendToTaskEngine(msg) }.join()

            return
        }

        if (handler.perTag != null) {
            val msg = RetryTaskPerTag(
                taskTag = handler.perTag!!,
                taskName = TaskName(handler.klass.name),
                methodName = null,
                methodParameterTypes = null,
                methodParameters = null,
                taskTags = handler.taskTags,
                taskOptions = handler.taskOptions,
                taskMeta = handler.taskMeta
            )
            GlobalScope.future { sendToTaskTagEngine(msg) }.join()

            return
        }
    }

    private fun <T : Any> cancelWorkflow(handler: WorkflowProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "retry")

        if (handler.perWorkflowId != null) {
            val msg = CancelWorkflow(
                workflowId = handler.perWorkflowId!!,
                workflowName = WorkflowName(handler.klass.name)
            )
            GlobalScope.future { sendToWorkflowEngine(msg) }.join()

            return
        }

        if (handler.perTag != null) {
            val msg = CancelWorkflowPerTag(
                workflowTag = handler.perTag!!,
                workflowName = WorkflowName(handler.klass.name)
            )
            GlobalScope.future { sendToWorkflowTagEngine(msg) }.join()

            return
        }
    }

    private fun <T : Any> retryWorkflow(handler: WorkflowProxyHandler<T>) {
        if (handler.isNew()) throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "retry")

        TODO("Not yet implemented")
    }
}
