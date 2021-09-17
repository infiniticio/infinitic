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
import io.infinitic.exceptions.clients.NotAnInterfaceException
import java.time.Duration
import java.time.Instant
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KFunction7
import kotlin.reflect.KFunction8
import kotlin.reflect.KFunction9
import kotlin.reflect.jvm.javaMethod

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
    ): T = NewTaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options,
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /*
     * Stub task
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
    ): T = NewWorkflowProxyHandler(
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
    fun <R : Any?> dispatch(method: KFunction0<R>): () -> Deferred<R> =
        { dispatch { method.check().call() } }

    fun <S1, R : Any?> dispatch(method: KFunction1<S1, R>): (S1) -> Deferred<R> =
        { s: S1 -> dispatch { method.check().call(s) } }

    fun <S1, S2, R : Any?> dispatch(method: KFunction2<S1, S2, R>): (S1, S2) -> Deferred<R> =
        { s1: S1, s2: S2 -> dispatch { method.check().call(s1, s2) } }

    fun <S1, S2, S3, R : Any?> dispatch(method: KFunction3<S1, S2, S3, R>): (S1, S2, S3) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3 -> dispatch { method.check().call(s1, s2, s3) } }

    fun <S1, S2, S3, S4, R : Any?> dispatch(method: KFunction4<S1, S2, S3, S4, R>): (S1, S2, S3, S4) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4 -> dispatch { method.check().call(s1, s2, s3, s4) } }

    fun <S1, S2, S3, S4, S5, R : Any?> dispatch(method: KFunction5<S1, S2, S3, S4, S5, R>): (S1, S2, S3, S4, S5) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5 -> dispatch { method.check().call(s1, s2, s3, s4, s5) } }

    fun <S1, S2, S3, S4, S5, S6, R : Any?> dispatch(method: KFunction6<S1, S2, S3, S4, S5, S6, R>): (S1, S2, S3, S4, S5, S6) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6 -> dispatch { method.check().call(s1, s2, s3, s4, s5, s6) } }

    fun <S1, S2, S3, S4, S5, S6, S7, R : Any?> dispatch(method: KFunction7<S1, S2, S3, S4, S5, S6, S7, R>): (S1, S2, S3, S4, S5, S6, S7) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7 -> dispatch { method.check().call(s1, s2, s3, s4, s5, s6, s7) } }

    fun <S1, S2, S3, S4, S5, S6, S7, S8, R : Any?> dispatch(method: KFunction8<S1, S2, S3, S4, S5, S6, S7, S8, R>): (S1, S2, S3, S4, S5, S6, S7, S8) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8 -> dispatch { method.check().call(s1, s2, s3, s4, s5, s6, s7, s8) } }

    fun <S1, S2, S3, S4, S5, S6, S7, S8, S9, R : Any?> dispatch(method: KFunction9<S1, S2, S3, S4, S5, S6, S7, S8, S9, R>): (S1, S2, S3, S4, S5, S6, S7, S8, S9) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8, s9: S9 -> dispatch { method.check().call(s1, s2, s3, s4, s5, s6, s7, s8, s9) } }

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

    private fun <R> KFunction<R>.check(): KFunction<R> = this.also {
        if (javaMethod?.declaringClass?.isInterface != true) throw NotAnInterfaceException(name, "dispatch")
    }

    private fun <R> dispatch(invoke: () -> R): Deferred<R> = dispatcher.dispatch(ProxyHandler.async(invoke)!!, false)
}
