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
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.data.methods.MethodReturnValue
import io.infinitic.common.proxies.ExistingTaskProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.CancelTaskPerTag
import io.infinitic.common.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.tags.messages.RetryTaskPerTag
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
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

    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewTaskProxyHandler(klass, options ?: TaskOptions(), TaskMeta(meta)) { dispatcher }.stub()

    /*
     * Create stub for a new task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = task(T::class.java, options, TaskMeta(meta))

    /*
     * Create stub for a new workflow
     */
    @JvmOverloads fun <T : Any> workflow(
        klass: Class<out T>,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewWorkflowProxyHandler(
        klass,
        options ?: WorkflowOptions(),
        WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /*
     * Create stub for a new workflow
     * (Kotlin way)
     */
    inline fun <reified T : Any> workflow(
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = workflow(T::class.java, options, meta)

    /*
     * Create stub for an existing task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        tag: String,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = ExistingTaskProxyHandler(
        klass,
        Tag(tag),
        options ?: TaskOptions(),
        TaskMeta(meta)
    ) { dispatcher }.stub()

    /*
     * Create stub for an existing task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(
        tag: String,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = task(T::class.java, tag, options, meta)

    /*
     * Create stub for an existing workflow
     */
    @JvmOverloads fun <T : Any> workflow(
        klass: Class<out T>,
        tag: String,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = ExistingWorkflowProxyHandler(
        klass,
        Tag(tag),
        options ?: WorkflowOptions(),
        WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /*
     * Create stub for an existing workflow
     * (kotlin way)
     */
    inline fun <reified T : Any> workflow(
        tag: String,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = workflow(T::class.java, tag, options, meta)

    /*
     *  Process (asynchronously) a task or a workflow
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): UUID {
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
        val msg = CancelTaskPerTag(
            tag = handle.tag,
            name = TaskName(handle.klass.name),
            taskReturnValue = MethodReturnValue.from(output)
        )
        GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()
    }

    private fun <T : Any> cancel(handler: ExistingWorkflowProxyHandler<T>, output: Any?) {
        val msg = CancelWorkflowPerTag(
            tag = handler.tag,
            name = WorkflowName(handler.klass.name),
            workflowReturnValue = MethodReturnValue.from(output)
        )
        GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()
    }

    private fun <T : Any> retry(handler: ExistingTaskProxyHandler<T>) {
        val msg = RetryTaskPerTag(
            tag = handler.tag,
            name = TaskName(handler.klass.name),
            methodName = handler.method?.let { MethodName.from(it) },
            methodParameterTypes = handler.method?.let { MethodParameterTypes.from(it) },
            methodParameters = handler.method?.let { MethodParameters.from(it, handler.args) },
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )
        GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()
    }

    private fun <T : Any> retry(handler: ExistingWorkflowProxyHandler<T>) {
        TODO("Not yet implemented")
    }
}
