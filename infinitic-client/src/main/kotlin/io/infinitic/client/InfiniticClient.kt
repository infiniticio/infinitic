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
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.exceptions.IncorrectExistingStub
import io.infinitic.common.tasks.exceptions.IncorrectNewStub
import io.infinitic.common.tasks.exceptions.NotAStub
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import java.lang.reflect.Proxy

@Suppress("MemberVisibilityCanBePrivate", "unused")
open class InfiniticClient(val clientOutput: ClientOutput) {
    /*
     * Create stub for a new task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = NewTaskProxyHandler(klass, options, meta, clientOutput).instance()

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
    ): T = NewWorkflowProxyHandler(klass, options, meta, clientOutput).instance()

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
    ): T = ExistingTaskProxyHandler(klass, id, options, meta, clientOutput).instance()

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
    ): T = ExistingWorkflowProxyHandler(klass, id, options, meta, clientOutput).instance()

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

        // collect method and args
        proxy.method()

        return when (val handler = Proxy.getInvocationHandler(proxy)) {
            is NewTaskProxyHandler<*> -> handler.startTask()
            is NewWorkflowProxyHandler<*> -> handler.startWorkflow()
            is ExistingTaskProxyHandler<*> -> throw IncorrectExistingStub(proxy::class.java.name, "async", "task")
            is ExistingWorkflowProxyHandler<*> -> throw IncorrectExistingStub(proxy::class.java.name, "async", "workflow")
            else -> throw RuntimeException("Unknown handler")
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
            is ExistingTaskProxyHandler<*> -> handler.cancelTask(output)
            is ExistingWorkflowProxyHandler<*> -> handler.cancelWorkflowAsync(output)
            else -> throw RuntimeException("Unknown handler")
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
            is ExistingTaskProxyHandler<*> -> handler.retryTask()
            is ExistingWorkflowProxyHandler<*> -> handler.retryWorkflowTask()
            else -> throw RuntimeException("Unknown handler")
        }
    }
}
