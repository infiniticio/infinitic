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

import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.exceptions.clients.InvalidStubException
import java.time.Duration
import java.time.Instant

@Suppress("unused")
abstract class Workflow {
    lateinit var context: WorkflowContext
    lateinit var dispatcher: WorkflowDispatcher

    /**
     *  Create a stub for a task
     */
    @JvmOverloads
    fun <T : Any> taskStub(
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

    /**
     *  Create a stub for a workflow
     */
    @JvmOverloads
    fun <T : Any> workflowStub(
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

    /**
     *  Start a task or workflow without parameter
     */
    @JvmOverloads
    fun <R> dispatch(
        method: Function0<R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With0<R> = With0 {
        dispatch(tags, options, meta) { method.invoke() }
    }

    /**
     *  Start a task or workflow with 1 parameter
     */
    @JvmOverloads
    fun <P1, R> dispatch(
        method: Function1<P1, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With1<P1, R> = With1 { p1 ->
        dispatch(tags, options, meta) { method.invoke(p1) }
    }

    /**
     *  Start a task or workflow with 2 parameters
     */
    @JvmOverloads
    fun <P1, P2, R : Any?> dispatch(
        method: Function2<P1, P2, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With2<P1, P2, R> = With2 { p1, p2 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2) }
    }

    /**
     *  Start a task or workflow with 3 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, R : Any?> dispatch(
        method: Function3<P1, P2, P3, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With3<P1, P2, P3, R> = With3 { p1, p2, p3 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3) }
    }

    /**
     *  Start a task or workflow with 4 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: Function4<P1, P2, P3, P4, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With4<P1, P2, P3, P4, R> = With4 { p1, p2, p3, p4 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4) }
    }

    /**
     *  Start a task or workflow with 5 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
        method: Function5<P1, P2, P3, P4, P5, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With5<P1, P2, P3, P4, P5, R> = With5 { p1, p2, p3, p4, p5 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4, p5) }
    }

    /**
     *  Start a task or workflow with 6 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
        method: Function6<P1, P2, P3, P4, P5, P6, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With6<P1, P2, P3, P4, P5, P6, R> = With6 { p1, p2, p3, p4, p5, p6 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4, p5, p6) }
    }

    /**
     *  Start a task or workflow with 7 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
        method: Function7<P1, P2, P3, P4, P5, P6, P7, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With7<P1, P2, P3, P4, P5, P6, P7, R> = With7 { p1, p2, p3, p4, p5, p6, p7 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4, p5, p6, p7) }
    }

    /**
     *  Start a task or workflow with 8 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
        method: Function8<P1, P2, P3, P4, P5, P6, P7, P8, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With8<P1, P2, P3, P4, P5, P6, P7, P8, R> = With8 { p1, p2, p3, p4, p5, p6, p7, p8 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }
    }

    /**
     *  Start a task or workflow with 9 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
        method: Function9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): With9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> = With9 { p1, p2, p3, p4, p5, p6, p7, p8, p9 ->
        dispatch(tags, options, meta) { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }
    }

    /**
     *  Create a channel
     */
    fun <T : Any> channel(): Channel<T> = ChannelImpl { dispatcher }

    /**
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S> = dispatcher.async(branch)

    /**
     * Create an inline task
     */
    fun <S> inline(task: () -> S): S = dispatcher.inline(task)

    /**
     * Create a timer from a duration
     */
    fun timer(duration: Duration): Deferred<Instant> = dispatcher.timer(duration)

    /**
     * Create a timer from an instant
     */
    fun timer(instant: Instant): Deferred<Instant> = dispatcher.timer(instant)

    private fun <R> dispatch(
        tags: Set<String>?,
        options: JobOptions?,
        meta: Map<String, ByteArray>?,
        invoke: () -> R
    ): Deferred<R> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, false, tags, options, meta)
    }
}
