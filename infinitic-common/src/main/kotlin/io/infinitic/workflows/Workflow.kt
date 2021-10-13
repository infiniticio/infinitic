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

import io.infinitic.annotations.Name
import io.infinitic.common.proxies.GetTaskProxyHandler
import io.infinitic.common.proxies.GetWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.workflows.data.channels.ChannelImpl
import io.infinitic.common.workflows.data.workflows.WorkflowId
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
    protected fun <T : Any> newTask(
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
    protected fun <T : Any> newWorkflow(
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
     *  Create a stub for an existing task targeted by id
     */
    fun <T : Any> getTaskById(
        klass: Class<out T>,
        id: String
    ): T = GetTaskProxyHandler(
        klass = klass,
        TaskId(id),
        null
    ) { dispatcher }.stub()

    /**
     *  Create a stub for existing task targeted by tag
     */
    fun <T : Any> getTaskByTag(
        klass: Class<out T>,
        tag: String
    ): T = GetTaskProxyHandler(
        klass = klass,
        null,
        TaskTag(tag)
    ) { dispatcher }.stub()

    /**
     *  Create a stub for an existing workflow targeted by id
     */
    protected fun <T : Any> getWorkflowById(
        klass: Class<out T>,
        id: String
    ): T = GetWorkflowProxyHandler(
        klass = klass,
        WorkflowId(id),
        null
    ) { dispatcher }.stub()

    /**
     *  Create a stub for existing workflow targeted by tag
     */
    protected fun <T : Any> getWorkflowByTag(
        klass: Class<out T>,
        tag: String
    ): T = GetWorkflowProxyHandler(
        klass = klass,
        null,
        WorkflowTag(tag)
    ) { dispatcher }.stub()

    /**
     *  Dispatch without parameter a task or workflow returning an object
     */
    protected fun <R : Any?> dispatch(
        method: () -> R
    ): Deferred<R> = start { method.invoke() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning an object
     */
    protected fun <P1, R : Any?> dispatch(
        method: (p1: P1) -> R,
        p1: P1
    ): Deferred<R> = start { method.invoke(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, R : Any?> dispatch(
        method: (p1: P1, p2: P2) -> R,
        p1: P1,
        p2: P2
    ): Deferred<R> = start { method.invoke(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3) -> R,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<R> = start { method.invoke(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning an object
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
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
    ): Deferred<R> = start { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Dispatch without parameter a task or workflow returning void
     */
    protected fun dispatchVoid(
        method: Consumer0
    ): Deferred<Void> = startVoid { method.apply() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning void
     */
    protected fun <P1> dispatchVoid(
        method: Consumer1<P1>,
        p1: P1
    ): Deferred<Void> = startVoid { method.apply(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning void
     */
    protected fun <P1, P2> dispatchVoid(
        method: Consumer2<P1, P2>,
        p1: P1,
        p2: P2
    ): Deferred<Void> = startVoid { method.apply(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3> dispatchVoid(
        method: Consumer3<P1, P2, P3>,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4> dispatchVoid(
        method: Consumer4<P1, P2, P3, P4>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4, P5> dispatchVoid(
        method: Consumer5<P1, P2, P3, P4, P5>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4, P5, P6> dispatchVoid(
        method: Consumer6<P1, P2, P3, P4, P5, P6>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoid(
        method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoid(
        method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning void
     */
    protected fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoid(
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
    ): Deferred<Void> = startVoid { method.apply(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Create a channel
     */
    protected fun <T : Any> channel(): Channel<T> = ChannelImpl { dispatcher }

    /**
     * Run inline task
     */
    protected fun <S> inline(task: () -> S): S = dispatcher.inline(task)

    /**
     * Run inline task returning void
     */
    protected fun inlineVoid(task: Consumer0): Unit = dispatcher.inline { task.apply() }

    /**
     * Create a timer from a duration
     */
    protected fun timer(duration: Duration): Deferred<Instant> = dispatcher.timer(duration)

    /**
     * Create a timer from an instant
     */
    protected fun timer(instant: Instant): Deferred<Instant> = dispatcher.timer(instant)

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

    // from klass for the given workflow name
    protected fun findClassPerWorkflowName() = try {
        Class.forName(context.name)
    } catch (e: ClassNotFoundException) {
        findClassPerAnnotationName(this::class.java, context.name)
    }

    // from klass, search for a given @Name annotation
    protected fun findClassPerAnnotationName(klass: Class<*>, name: String): Class<*>? {
        var clazz = klass

        do {
            // has current clazz the right @Name annotation?
            if (clazz.getAnnotation(Name::class.java)?.name == name) return clazz

            // has any of the interfaces the right @Name annotation?
            clazz.interfaces.forEach { interfaze ->
                findClassPerAnnotationName(interfaze, name)?.also { return it }
            }

            // if not, inspect the superclass
            clazz = clazz.superclass ?: break
        } while (Object::class.java.name != clazz.canonicalName)

        return null
    }
}
