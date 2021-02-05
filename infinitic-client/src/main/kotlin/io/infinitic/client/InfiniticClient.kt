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

import io.infinitic.client.proxies.ExistingTaskProxyHandler
import io.infinitic.client.proxies.ExistingWorkflowProxyHandler
import io.infinitic.client.proxies.NewTaskProxyHandler
import io.infinitic.client.proxies.NewWorkflowProxyHandler
import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.exceptions.IncorrectExistingStub
import io.infinitic.common.tasks.exceptions.IncorrectNewStub
import io.infinitic.common.tasks.exceptions.NoMethodCall
import io.infinitic.common.tasks.exceptions.NotAStub
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Proxy

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class InfiniticClient(val clientOutput: ClientOutput) {

    // should be replay = 0
    // but replay = 1 make tests much easier, as we can emit a message before listening
    private val responseFlow = MutableSharedFlow<ClientResponseMessage>(replay = 1)

    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = NewTaskProxyHandler(klass, options, meta, this).stub()

    /*
     * Create stub for a new task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = task(T::class.java, options, meta)

    /*
     * Create stub for a new workflow
     */
    @JvmOverloads fun <T : Any> workflow(
        klass: Class<out T>,
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta()
    ): T = NewWorkflowProxyHandler(klass, options, meta, this).stub()

    /*
     * Create stub for a new workflow
     * (Kotlin way)
     */
    inline fun <reified T : Any> workflow(
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta()
    ): T = workflow(T::class.java, options, meta)

    /*
     * Create stub for an existing task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        id: String,
        options: TaskOptions? = null,
        meta: TaskMeta? = null
    ): T = ExistingTaskProxyHandler(klass, id, options, meta, this).stub()

    /*
     * Create stub for an existing task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(
        id: String,
        options: TaskOptions? = null,
        meta: TaskMeta? = null
    ): T = task(T::class.java, id, options, meta)

    /*
     * Create stub for an existing workflow
     */
    @JvmOverloads fun <T : Any> workflow(
        klass: Class<out T>,
        id: String,
        options: WorkflowOptions? = null,
        meta: WorkflowMeta? = null
    ): T = ExistingWorkflowProxyHandler(klass, id, options, meta, this).stub()

    /*
     * Create stub for an existing workflow
     * (kotlin way)
     */
    inline fun <reified T : Any> workflow(
        id: String,
        options: WorkflowOptions? = null,
        meta: WorkflowMeta? = null
    ): T = workflow(T::class.java, id, options, meta)

    /*
     *  Process (asynchronously) a task or a workflow
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): String {
        if (proxy !is Proxy) throw NotAStub(proxy::class.java.name, "async")

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is NewTaskProxyHandler<*> -> {
                handler.isSync = false
                proxy.method()
                startTaskAsync(handler)
            }
            is NewWorkflowProxyHandler<*> -> {
                handler.isSync = false
                proxy.method()
                startWorkflowAsync(handler)
            }
            is ExistingTaskProxyHandler<*> -> {
                throw IncorrectExistingStub(proxy::class.java.name, "async", "task")
            }
            is ExistingWorkflowProxyHandler<*> -> {
                throw IncorrectExistingStub(proxy::class.java.name, "async", "workflow")
            }
            else -> throw RuntimeException()
        }
    }

    /*
     *  Cancel a task or a workflow
     */
    fun <T : Any> cancel(proxy: T, output: Any? = null) {
        if (proxy !is Proxy) throw NotAStub(proxy::class.java.name, "cancel")

        when (val handler = Proxy.getInvocationHandler(proxy)) {
            is NewTaskProxyHandler<*> -> throw IncorrectNewStub(proxy::class.java.name, "cancel", "task")
            is NewWorkflowProxyHandler<*> -> throw IncorrectNewStub(proxy::class.java.name, "cancel", "workflow")
            is ExistingTaskProxyHandler<*> -> cancelTask(handler, output)
            is ExistingWorkflowProxyHandler<*> -> cancelWorkflow(handler, output)
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
            is NewTaskProxyHandler<*> -> throw IncorrectNewStub(proxy::class.java.name, "retry", "task")
            is NewWorkflowProxyHandler<*> -> throw IncorrectNewStub(proxy::class.java.name, "retry", "workflow")
            is ExistingTaskProxyHandler<*> -> retryTask(handler)
            is ExistingWorkflowProxyHandler<*> -> retryWorkflowTask(handler)
            else -> throw RuntimeException()
        }
    }

    suspend fun handle(message: ClientResponseMessage) = responseFlow.emit(message)

    internal fun startTask(handler: NewTaskProxyHandler<*>): Any? {
        // dispatch
        val id = startTaskAsync(handler, true)

        // wait for result
        val taskCompleted = runBlocking {
            // listen all responses, up to right one
            responseFlow.first {
                it is TaskCompleted && "${it.taskId}" == id
            }
        } as TaskCompleted

        return taskCompleted.taskOutput.get()
    }

    internal fun startWorkflow(handler: NewWorkflowProxyHandler<*>): Any? {
        // dispatch
        val id = startWorkflowAsync(handler, true)

        // wait for result
        val workflowCompleted = runBlocking {
            // listen all responses, up to right one
            responseFlow.first {
                it is WorkflowCompleted && "${it.workflowId}" == id
            }
        } as WorkflowCompleted

        return workflowCompleted.workflowOutput.get()
    }

    private fun startTaskAsync(handler: NewTaskProxyHandler<*>, isSync: Boolean = false): String {
        if (handler.method == null) throw NoMethodCall(handler.klass.name, "async")

        val msg = DispatchTask(
            taskId = TaskId(),
            clientName = clientOutput.clientName,
            clientWaiting = isSync,
            taskName = TaskName.from(handler.method!!),
            methodName = MethodName.from(handler.method!!),
            methodParameterTypes = MethodParameterTypes.from(handler.method!!),
            methodInput = MethodInput.from(handler.method!!, handler.args),
            workflowId = null,
            methodRunId = null,
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, 0F) }.join()

        // reset handler for reuse
        handler.reset()

        return "${msg.taskId}"
    }

    private fun <T : Any> startWorkflowAsync(handler: NewWorkflowProxyHandler<T>, isSync: Boolean = false): String {
        if (handler.method == null) throw NoMethodCall(handler.klass.name, "async")

        val msg = DispatchWorkflow(
            workflowId = WorkflowId(),
            clientName = clientOutput.clientName,
            clientWaiting = isSync,
            workflowName = WorkflowName.from(handler.method!!),
            methodName = MethodName.from(handler.method!!),
            methodParameterTypes = MethodParameterTypes.from(handler.method!!),
            methodInput = MethodInput.from(handler.method!!, handler.args),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowMeta = handler.workflowMeta,
            workflowOptions = handler.workflowOptions
        )
        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, 0F) }.join()

        // reset handler for reuse
        handler.reset()

        return "${msg.workflowId}"
    }

    private fun <T : Any> cancelTask(handle: ExistingTaskProxyHandler<T>, output: Any?) {
        val msg = CancelTask(
            taskId = TaskId(handle.taskId),
            taskOutput = MethodOutput.from(output)
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, 0F) }.join()
    }

    private fun <T : Any> cancelWorkflow(handler: ExistingWorkflowProxyHandler<T>, output: Any?) {
        val msg = CancelWorkflow(
            workflowId = WorkflowId(handler.workflowId),
            clientName = null,
            workflowOutput = MethodOutput.from(output)
        )
        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, 0F) }.join()
    }

    private fun <T : Any> retryTask(handler: ExistingTaskProxyHandler<T>) {
        val msg = RetryTask(
            taskId = TaskId(handler.taskId),
            taskName = TaskName(handler.klass.name),
            methodName = handler.method?.let { MethodName.from(it) },
            methodParameterTypes = handler.method?.let { MethodParameterTypes.from(it) },
            methodInput = handler.method?.let { MethodInput.from(it, handler.args) },
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, 0F) }.join()
    }

    private fun <T : Any> retryWorkflowTask(handler: ExistingWorkflowProxyHandler<T>) {
        TODO("Not yet implemented")
    }
}
