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

package io.infinitic.workflows

import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import java.time.Duration
import java.time.Instant

abstract class Workflow {
    lateinit var context: WorkflowTaskContext

    /*
     *  Stub task
     */
    @JvmOverloads fun <T : Any> task(
        klass: Class<out T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = NewTaskProxyHandler(klass, options, meta) { context }.stub()

    /*
     * Stub task
     * (Kotlin way)
     */
    inline fun <reified T : Any> task(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta()
    ): T = NewTaskProxyHandler(T::class.java, options, meta) { context }.stub()

    /*
     *  Stub workflow
     */
    @JvmOverloads fun <T : Any> workflow(
        klass: Class<out T>,
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta()
    ): T = NewWorkflowProxyHandler(klass, options, meta) { context }.stub()

    /*
     *  Stub workflow
     * (Kotlin way)
     */
    inline fun <reified T : Any> workflow(
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta()
    ): T = NewWorkflowProxyHandler(T::class.java, options, meta) { context }.stub()

    /*
     *  Dispatch a task or a workflow asynchronously
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S> = context.async(proxy, method)

    /*
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S> = context.async(branch)

    /*
     * Create an inline task
     */
    fun <S> inline(task: () -> S): S = context.inline(task)

    /*
     * Create a timer from a duration
     */
    fun timer(duration: Duration): Deferred<Instant> = context.timer(duration)

    /*
     * Create a timer from an instant
     */
    fun timer(instant: Instant): Deferred<Instant> = context.timer(instant)
}
