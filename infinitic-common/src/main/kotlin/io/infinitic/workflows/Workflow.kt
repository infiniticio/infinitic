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

import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import java.time.Duration
import java.time.Instant

@Suppress("unused")
abstract class Workflow {
    lateinit var context: WorkflowContext
    lateinit var dispatcher: WorkflowDispatcher

    /*
     *  Create a channel
     */
    fun <T : Any> channel(): Channel<T> = ChannelImpl { dispatcher }

    /*
     * Stub task
     */
    @JvmOverloads fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions = TaskOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = TaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options,
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /*
     * (Kotlin) Stub task
     */
    inline fun <reified T : Any> newTask(
        tags: Set<String> = setOf(),
        options: TaskOptions = TaskOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = newTask(T::class.java, tags, options, meta)

    /*
     * Stub workflow
     */
    @JvmOverloads fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions = WorkflowOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = WorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options,
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /*
     * Stub workflow
     * (Kotlin way)
     */
    inline fun <reified T : Any> newWorkflow(
        tags: Set<String> = setOf(),
        options: WorkflowOptions = WorkflowOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = newWorkflow(T::class.java, tags, options, meta)

    /*
     *  Dispatch a task or a workflow asynchronously
     */
    fun <T : Any, S> async(proxy: T, method: T.() -> S): Deferred<S> = dispatcher.dispatch(proxy, method)

    /*
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S> = dispatcher.async(branch)

    /*
     * Create an inline task
     */
    fun <S> inline(task: () -> S): S = dispatcher.inline(task)

    /*
     * Create a timer from a duration
     */
    fun timer(duration: Duration): Deferred<Instant> = dispatcher.timer(duration)

    /*
     * Create a timer from an instant
     */
    fun timer(instant: Instant): Deferred<Instant> = dispatcher.timer(instant)
}
