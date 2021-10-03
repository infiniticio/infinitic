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
import io.infinitic.common.proxies.ProxyHandler
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
    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions = TaskOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewTaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options,
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /**
     *  Create a stub for a workflow
     */
    @JvmOverloads
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions = WorkflowOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewWorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options,
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /**
     *  Dispatch without parameter a task or workflow returning an object
     */
    fun <R : Any?> dispatch(
        method: () -> R
    ): Deferred<R> = start() { method.invoke() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning an object
     */
    fun <P1, R : Any?> dispatch(
        method: (p1: P1) -> R,
        p1: P1
    ): Deferred<R> = start() { method.invoke(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning an object
     */
    fun <P1, P2, R : Any?> dispatch(
        method: (p1: P1, p2: P2) -> R,
        p1: P1,
        p2: P2
    ): Deferred<R> = start() { method.invoke(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3) -> R,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<R> = start() { method.invoke(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9
    ): Deferred<R> = start() { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Dispatch without parameter a task or workflow returning void
     */
    fun dispatchVoid(
        method: Consumer0
    ): Deferred<Void> = startVoid() { method.apply() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning void
     */
    fun <P1> dispatchVoid(
        method: Consumer1<P1>,
        p1: P1
    ): Deferred<Void> = startVoid() { method.apply(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning void
     */
    fun <P1, P2> dispatchVoid(
        method: Consumer2<P1, P2>,
        p1: P1,
        p2: P2
    ): Deferred<Void> = startVoid() { method.apply(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning void
     */
    fun <P1, P2, P3> dispatchVoid(
        method: Consumer3<P1, P2, P3>,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4> dispatchVoid(
        method: Consumer4<P1, P2, P3, P4>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5> dispatchVoid(
        method: Consumer5<P1, P2, P3, P4, P5>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6> dispatchVoid(
        method: Consumer6<P1, P2, P3, P4, P5, P6>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoid(
        method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoid(
        method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoid(
        method: Consumer9<P1, P2, P3, P4, P5, P6, P7, P8, P9>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8,
        p9: P9
    ): Deferred<Void> = startVoid() { method.apply(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Create a channel
     */
    fun <T : Any> channel(): Channel<T> = ChannelImpl { dispatcher }

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

    /*
     * Create an async branch
     */
    fun <S> async(branch: () -> S): Deferred<S> = dispatcher.async(branch)

    private fun <R> start(
        invoke: () -> R
    ): Deferred<R> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, false)
    }

    private fun startVoid(
        invoke: () -> Unit
    ): Deferred<Void> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, false)
    }
}
