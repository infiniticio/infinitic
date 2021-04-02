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

import io.infinitic.client.output.ClientOutput
import io.infinitic.client.output.LoggedClientOutput
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.CancelTaskPerTag
import io.infinitic.common.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.tags.messages.RetryTaskPerTag
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.exceptions.CanNotReuseTaskStub
import io.infinitic.exceptions.CanNotReuseWorkflowStub
import io.infinitic.exceptions.CanNotUseNewTaskStub
import io.infinitic.exceptions.CanNotUseNewWorkflowStub
import io.infinitic.exceptions.IncorrectExistingStub
import io.infinitic.exceptions.NotAStub
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.lang.reflect.Proxy
import java.util.UUID

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class Client {

    companion object {
        fun with(clientOutput: ClientOutput) = Client().apply { this.clientOutput = clientOutput }
    }

    private lateinit var dispatcher: ClientDispatcher
    private lateinit var _clientOutput: ClientOutput

    var clientOutput: ClientOutput
        set(value) {
            _clientOutput = LoggedClientOutput(value)
            dispatcher = ClientDispatcher(_clientOutput)
        }
        get() = _clientOutput

    suspend fun handle(message: ClientMessage) = dispatcher.handle(message)

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
        tags = tags.map { Tag(it) }.toSet(),
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
        tags = tags?.map { Tag(it) }?.toSet(),
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
        tags = tags?.map { Tag(it) }?.toSet(),
        taskOptions = options,
        taskMeta = meta?.let { TaskMeta(it) },
        perTaskId = null,
        perTag = Tag(tag)
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
        tags = tags.map { Tag(it) }.toSet(),
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
        tags = tags?.map { Tag(it) }?.toSet(),
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
        perTag = Tag(tag),
        perWorkflowId = null,
        tags = tags?.map { Tag(it) }?.toSet(),
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
    fun <T : Any, S> async(proxy: T, method: T.() -> S): UUID {
        if (proxy !is Proxy) throw NotAStub(proxy::class.java.name, "async")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> {
                if (! handler.isNew()) throw CanNotReuseTaskStub(handler.klass.name)
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is WorkflowProxyHandler<*> -> {
                if (! handler.isNew()) throw CanNotReuseWorkflowStub(handler.klass.name)
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
    fun <T : Any> cancel(proxy: T, returnValue: Any? = null) {
        if (proxy !is Proxy) throw NotAStub(proxy::class.java.name, "cancel")

        when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> when (handler.isNew()) {
                true -> throw CanNotUseNewTaskStub(handler.klass.name, "cancel")
                false -> cancel(handler, returnValue)
            }
            is WorkflowProxyHandler<*> -> when (handler.isNew()) {
                true -> throw CanNotUseNewWorkflowStub(handler.klass.name, "cancel")
                false -> cancel(handler, returnValue)
            }
            is SendChannelProxyHandler<*> -> throw IncorrectExistingStub(handler.klass.name, "cancel")
            else -> throw RuntimeException()
        }
    }

    /*
     * Retry a task or a workflowTask
     * when a non-null parameter is provided, it will supersede current one
     */
    fun <T : Any> retry(proxy: T) {
        if (proxy !is Proxy) throw NotAStub(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is TaskProxyHandler<*> -> when (handler.isNew()) {
                true -> throw CanNotUseNewTaskStub(handler.klass.name, "retry")
                false -> retry(handler)
            }
            is WorkflowProxyHandler<*> -> when (handler.isNew()) {
                true -> throw CanNotUseNewWorkflowStub(handler.klass.name, "retry")
                false -> retry(handler)
            }
            is SendChannelProxyHandler<*> -> throw IncorrectExistingStub(handler.klass.name, "retry")
            else -> throw RuntimeException()
        }
    }

    private fun <T : Any> cancel(handler: TaskProxyHandler<T>, output: Any?) {
        if (handler.isNew()) throw CanNotUseNewTaskStub(handler.klass.name, "cancel")

        when (handler.perTaskId) {
            null -> {
                val msg = CancelTaskPerTag(
                    tag = handler.perTag!!,
                    name = TaskName(handler.klass.name),
                    taskReturnValue = MethodReturnValue.from(output)
                )
                GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()
            }
            else -> {
                val msg = CancelTask(
                    taskId = handler.perTaskId!!,
                    taskName = TaskName(handler.klass.name),
                    taskReturnValue = MethodReturnValue.from(output)
                )
                GlobalScope.future { clientOutput.sendToTaskEngine(msg) }.join()
            }
        }
    }

    private fun <T : Any> retry(handler: TaskProxyHandler<T>) {
        if (handler.isNew()) throw CanNotUseNewTaskStub(handler.klass.name, "retry")

        if (handler.perTaskId != null) {
            val msg = RetryTask(
                taskId = handler.perTaskId!!,
                taskName = TaskName(handler.klass.name),
                methodName = null,
                methodParameterTypes = null,
                methodParameters = null,
                tags = handler.tags,
                taskOptions = handler.taskOptions,
                taskMeta = handler.taskMeta
            )
            GlobalScope.future { clientOutput.sendToTaskEngine(msg) }.join()

            return
        }

        if (handler.perTag != null) {
            val msg = RetryTaskPerTag(
                tag = handler.perTag!!,
                name = TaskName(handler.klass.name),
                methodName = null,
                methodParameterTypes = null,
                methodParameters = null,
                tags = handler.tags,
                taskOptions = handler.taskOptions,
                taskMeta = handler.taskMeta
            )
            GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()

            return
        }
    }

    private fun <T : Any> cancel(handler: WorkflowProxyHandler<T>, output: Any?) {
        if (handler.isNew()) throw CanNotUseNewWorkflowStub(handler.klass.name, "retry")

        if (handler.perWorkflowId != null) {
            val msg = CancelWorkflow(
                workflowId = handler.perWorkflowId!!,
                workflowName = WorkflowName(handler.klass.name),
                workflowReturnValue = MethodReturnValue.from(output)
            )
            GlobalScope.future { clientOutput.sendToWorkflowEngine(msg) }.join()

            return
        }

        if (handler.perTag != null) {
            val msg = CancelWorkflowPerTag(
                tag = handler.perTag!!,
                name = WorkflowName(handler.klass.name),
                workflowReturnValue = MethodReturnValue.from(output)
            )
            GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()

            return
        }
    }

    private fun <T : Any> retry(handler: WorkflowProxyHandler<T>) {
        TODO("Not yet implemented")
    }
}
