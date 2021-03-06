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

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.proxies.ExistingTaskProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.exceptions.IncorrectExistingStub
import io.infinitic.exceptions.NotAStub
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.lang.reflect.Proxy

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class Client(val clientOutput: ClientOutput) {

    private val dispatcher = ClientDispatcher(clientOutput)

    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = NewTaskProxyHandler(klass, options, meta) { dispatcher }.stub()

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
    ): T = NewWorkflowProxyHandler(klass, options, meta) { dispatcher }.stub()

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
    ): T = ExistingTaskProxyHandler(klass, id, options, meta) { dispatcher }.stub()

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
    ): T = ExistingWorkflowProxyHandler(klass, WorkflowId(id), options, meta) { dispatcher }.stub()

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
                dispatcher.dispatch(handler)
            }
            is NewWorkflowProxyHandler<*> -> {
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is ExistingTaskProxyHandler<*> -> {
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is ExistingWorkflowProxyHandler<*> -> {
                handler.isSync = false
                proxy.method()
                dispatcher.dispatch(handler)
            }
            is SendChannelProxyHandler<*> -> {
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
            is NewTaskProxyHandler<*> -> throw IncorrectExistingStub(handler.klass.name, "cancel")
            is NewWorkflowProxyHandler<*> -> throw IncorrectExistingStub(handler.klass.name, "cancel")
            is ExistingTaskProxyHandler<*> -> cancel(handler, returnValue)
            is ExistingWorkflowProxyHandler<*> -> cancel(handler, returnValue)
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
            is NewTaskProxyHandler<*> -> throw IncorrectExistingStub(proxy::class.java.name, "retry")
            is NewWorkflowProxyHandler<*> -> throw IncorrectExistingStub(proxy::class.java.name, "retry")
            is ExistingTaskProxyHandler<*> -> retry(handler)
            is ExistingWorkflowProxyHandler<*> -> retry(handler)
            is SendChannelProxyHandler<*> -> throw IncorrectExistingStub(handler.klass.name, "retry")
            else -> throw RuntimeException()
        }
    }

    suspend fun handle(message: ClientResponseMessage) = dispatcher.handle(message)

    private fun <T : Any> cancel(handle: ExistingTaskProxyHandler<T>, output: Any?) {
        val msg = CancelTask(
            taskId = TaskId(handle.taskId),
            taskName = TaskName.from(handle.klass),
            taskReturnValue = MethodReturnValue.from(output)
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, MillisDuration(0)) }.join()
    }

    private fun <T : Any> cancel(handler: ExistingWorkflowProxyHandler<T>, output: Any?) {
        val msg = CancelWorkflow(
            workflowId = handler.workflowId,
            clientName = null,
            workflowOutput = MethodReturnValue.from(output)
        )
        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, MillisDuration(0)) }.join()
    }

    private fun <T : Any> retry(handler: ExistingTaskProxyHandler<T>) {
        val msg = RetryTask(
            taskId = TaskId(handler.taskId),
            taskName = TaskName(handler.klass.name),
            methodName = handler.method?.let { MethodName.from(it) },
            methodParameterTypes = handler.method?.let { MethodParameterTypes.from(it) },
            methodParameters = handler.method?.let { MethodParameters.from(it, handler.args) },
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, MillisDuration(0)) }.join()
    }

    private fun <T : Any> retry(handler: ExistingWorkflowProxyHandler<T>) {
        TODO("Not yet implemented")
    }
}
