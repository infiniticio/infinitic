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

import io.infinitic.client.proxies.TaskProxyHandler
import io.infinitic.client.proxies.WorkflowProxyHandler
import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class InfiniticClient(val clientOutput: ClientOutput) {
    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = TaskProxyHandler(klass, options, meta, clientOutput).instance()

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
    ): T = WorkflowProxyHandler(klass, options, meta, clientOutput).instance()

    /*
     * Create stub for a new workflow
     * (kotlin way)
     */
    inline fun <reified T : Any> workflow(
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta()
    ): T = workflow(T::class.java, options, meta)

    /*
     *  Asynchronous processing of a task or a workflow
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): String {
        if (proxy !is Proxy) throw RuntimeException("not a proxy")

        val handler = Proxy.getInvocationHandler(proxy)

        // collect method and args
        proxy.method()

        return when (handler) {
            is TaskProxyHandler<*> -> handler.startTaskAsync()
            is WorkflowProxyHandler<*> -> handler.startWorkflowAsync()
            else -> throw RuntimeException("Unknown handler")
        }
    }

    /*
     * Retry a task (Java)
     * when a non-null parameter is provided, it will supersede current one
     */
    @JvmOverloads fun retryTaskAsync(
        id: String,
        name: TaskName? = null,
        methodName: MethodName? = null,
        methodParameterTypes: MethodParameterTypes? = null,
        methodInput: MethodInput? = null,
        taskOptions: TaskOptions? = null,
        taskMeta: TaskMeta? = null
    ): CompletableFuture<Unit> = GlobalScope.future {
        retryTask(id, name, methodName, methodParameterTypes, methodInput, taskOptions, taskMeta)
    }

    /*
     * Retry a task (Kotlin)
     * when a non-null parameter is provided, it will supersede current one
     */
    suspend fun retryTask(
        id: String,
        name: TaskName? = null,
        methodName: MethodName? = null,
        methodParameterTypes: MethodParameterTypes? = null,
        methodInput: MethodInput? = null,
        taskOptions: TaskOptions? = null,
        taskMeta: TaskMeta? = null
    ) {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = name,
            methodName = methodName,
            methodParameterTypes = methodParameterTypes,
            methodInput = methodInput,
            taskOptions = taskOptions,
            taskMeta = taskMeta
        )
        clientOutput.sendToTaskEngine(msg, 0F)
    }

    /*
     * Cancel a task (Java)
     */
    @JvmOverloads fun cancelTaskAsync(
        id: String,
        output: Any? = null
    ): CompletableFuture<Unit> = GlobalScope.future {
        cancelTask(id, output)
    }

    /*
     * Cancel a task (Kotlin)
     */
    suspend fun cancelTask(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelTask(
            taskId = TaskId(id),
            taskOutput = MethodOutput.from(output)
        )
        clientOutput.sendToTaskEngine(msg, 0F)
    }

    /*
     * Cancel a workflow (Java)
     */
    @JvmOverloads fun cancelWorkflowAsync(
        id: String,
        output: Any? = null
    ): CompletableFuture<Unit> = GlobalScope.future {
        cancelWorkflow(id, output)
    }

    /*
     * Cancel a workflow (Kotlin)
     */
    suspend fun cancelWorkflow(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelWorkflow(
            workflowId = WorkflowId(id),
            workflowOutput = MethodOutput.from(output)
        )
        clientOutput.sendToWorkflowEngineFn(msg, 0F)
    }
}
