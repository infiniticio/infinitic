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

package io.infinitic.common.clients

import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.tasks.TaskOptions
import io.infinitic.workflows.Consumer0
import io.infinitic.workflows.Consumer1
import io.infinitic.workflows.Consumer2
import io.infinitic.workflows.Consumer3
import io.infinitic.workflows.Consumer4
import io.infinitic.workflows.Consumer5
import io.infinitic.workflows.Consumer6
import io.infinitic.workflows.Consumer7
import io.infinitic.workflows.Consumer8
import io.infinitic.workflows.Consumer9
import io.infinitic.workflows.WorkflowOptions
import java.io.Closeable
import java.util.concurrent.CompletableFuture

interface InfiniticClient : Closeable {
    /**
     * Client's name
     * This name must be unique
     */
    val name: String

    /**
     *  Get last Deferred created by the call of a stub
     */
    val lastDeferred: Deferred<*>?

    /**
     * Close all resources used
     */
    override fun close()

    /**
     * Wait for all messages to be sent
     */
    fun join()

    suspend fun handle(message: ClientMessage)

    /**
     *  Create a stub for a new task
     */
    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String>? = null,
        options: TaskOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): T

    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String>? = null,
        options: TaskOptions? = null
    ): T = newTask(klass, tags, options, null)

    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String>? = null,
    ): T = newTask(klass, tags, null, null)

    fun <T : Any> newTask(
        klass: Class<out T>,
    ): T = newTask(klass, null, null, null)

    /**
     *  Create a stub for a new workflow
     */
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray>?,
    ): T

    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String>? = null,
        options: WorkflowOptions? = null
    ): T = newWorkflow(klass, tags, options, null)

    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String>? = null
    ): T = newWorkflow(klass, tags, null, null)

    fun <T : Any> newWorkflow(
        klass: Class<out T>
    ): T = newWorkflow(klass, null, null, null)

    /**
     *  Create a stub for an existing task targeted by id
     */
    fun <T : Any> getTaskById(
        klass: Class<out T>,
        id: String
    ): T

    /**
     *  Create a stub for existing task targeted by tag
     */
    fun <T : Any> getTaskByTag(
        klass: Class<out T>,
        tag: String
    ): T

    /**
     *  Create a stub for an existing workflow targeted by id
     */
    fun <T : Any> getWorkflowById(
        klass: Class<out T>,
        id: String
    ): T

    /**
     *  Create a stub for existing workflow targeted by tag
     */
    fun <T : Any> getWorkflowByTag(
        klass: Class<out T>,
        tag: String
    ): T

    /**
     *  Dispatch without parameter a task or workflow returning an object
     */
    fun <R : Any?> dispatchAsync(
        method: () -> R
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning an object
     */
    fun <P1, R : Any?> dispatchAsync(
        method: (p1: P1) -> R,
        p1: P1
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning an object
     */
    fun <P1, P2, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2) -> R,
        p1: P1,
        p2: P2
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3) -> R,
        p1: P1,
        p2: P2,
        p3: P3
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatchAsync(
        method: (p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatchAsync(
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
    ): CompletableFuture<Deferred<R>> = startAsync { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Dispatch without parameter a task or workflow returning an object
     */
    fun <R : Any?> dispatch(
        method: () -> R
    ): Deferred<R> = dispatchAsync(method).join()

    /**
     *  Dispatch with 1 parameter a task or workflow returning an object
     */
    fun <P1, R : Any?> dispatch(
        method: (p1: P1) -> R,
        p1: P1
    ): Deferred<R> = dispatchAsync(method, p1).join()

    /**
     *  Dispatch with 2 parameters a task or workflow returning an object
     */
    fun <P1, P2, R : Any?> dispatch(
        method: (p1: P1, p2: P2) -> R,
        p1: P1,
        p2: P2
    ): Deferred<R> = dispatchAsync(method, p1, p2).join()

    /**
     *  Dispatch with 3 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3) -> R,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3).join()

    /**
     *  Dispatch with 4 parameters a task or workflow returning an object
     */
    fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: (p1: P1, p2: P2, p3: P3, p4: P4) -> R,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4).join()

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
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5).join()

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
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6).join()

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
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7).join()

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
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7, p8).join()

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
    ): Deferred<R> = dispatchAsync(method, p1, p2, p3, p4, p5, p6, p7, p8, p9).join()

    /**
     *  Dispatch without parameter a task or workflow returning void
     */
    fun dispatchVoidAsync(
        method: Consumer0
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply() }

    /**
     *  Dispatch with 1 parameter a task or workflow returning void
     */
    fun <P1> dispatchVoidAsync(
        method: Consumer1<P1>,
        p1: P1
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1) }

    /**
     *  Dispatch with 2 parameters a task or workflow returning void
     */
    fun <P1, P2> dispatchVoidAsync(
        method: Consumer2<P1, P2>,
        p1: P1,
        p2: P2
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2) }

    /**
     *  Dispatch with 3 parameters a task or workflow returning void
     */
    fun <P1, P2, P3> dispatchVoidAsync(
        method: Consumer3<P1, P2, P3>,
        p1: P1,
        p2: P2,
        p3: P3
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3) }

    /**
     *  Dispatch with 4 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4> dispatchVoidAsync(
        method: Consumer4<P1, P2, P3, P4>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4) }

    /**
     *  Dispatch with 5 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5> dispatchVoidAsync(
        method: Consumer5<P1, P2, P3, P4, P5>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5) }

    /**
     *  Dispatch with 6 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6> dispatchVoidAsync(
        method: Consumer6<P1, P2, P3, P4, P5, P6>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6) }

    /**
     *  Dispatch with 7 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7> dispatchVoidAsync(
        method: Consumer7<P1, P2, P3, P4, P5, P6, P7>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6, p7) }

    /**
     *  Dispatch with 8 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8> dispatchVoidAsync(
        method: Consumer8<P1, P2, P3, P4, P5, P6, P7, P8>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4,
        p5: P5,
        p6: P6,
        p7: P7,
        p8: P8
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6, p7, p8) }

    /**
     *  Dispatch with 9 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9> dispatchVoidAsync(
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
    ): CompletableFuture<Deferred<Void>> = startVoidAsync { method.apply(p1, p2, p3, p4, p5, p6, p7, p8, p9) }

    /**
     *  Dispatch without parameter a task or workflow returning void
     */
    fun dispatchVoid(
        method: Consumer0
    ): Deferred<Void> = dispatchVoidAsync(method).join()

    /**
     *  Dispatch with 1 parameter a task or workflow returning void
     */
    fun <P1> dispatchVoid(
        method: Consumer1<P1>,
        p1: P1
    ): Deferred<Void> = dispatchVoidAsync(method, p1).join()

    /**
     *  Dispatch with 2 parameters a task or workflow returning void
     */
    fun <P1, P2> dispatchVoid(
        method: Consumer2<P1, P2>,
        p1: P1,
        p2: P2
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2).join()

    /**
     *  Dispatch with 3 parameters a task or workflow returning void
     */
    fun <P1, P2, P3> dispatchVoid(
        method: Consumer3<P1, P2, P3>,
        p1: P1,
        p2: P2,
        p3: P3
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3).join()

    /**
     *  Dispatch with 4 parameters a task or workflow returning void
     */
    fun <P1, P2, P3, P4> dispatchVoid(
        method: Consumer4<P1, P2, P3, P4>,
        p1: P1,
        p2: P2,
        p3: P3,
        p4: P4
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4).join()

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
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5).join()

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
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6).join()

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
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7).join()

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
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7, p8).join()

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
    ): Deferred<Void> = dispatchVoidAsync(method, p1, p2, p3, p4, p5, p6, p7, p8, p9).join()

    /**
     * Await a task or a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T
    ): Any?

    /**
     * Await a method from a running workflow targeted by its id and the methodRunId
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        methodRunId: String
    ): Any?

    /**
     * Cancel a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancelAsync(
        stub: T
    ): CompletableFuture<Unit>

    /**
     * Cancel a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T
    ): Unit = cancelAsync(stub).join()

    /**
     * Complete a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> completeAsync(
        stub: T,
        value: Any?
    ): CompletableFuture<Unit>

    /**
     * Complete a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        value: Any?
    ): Unit = completeAsync(stub, value).join()

    /**
     * Retry a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retryAsync(
        stub: T,
    ): CompletableFuture<Unit>

    /**
     * Retry a task or a workflow
     */
    fun <T : Any> retry(
        stub: T,
    ): Unit = retryAsync(stub).join()

    /**
     * get ids of a stub, associated to a specific tag
     */
    fun <T : Any> getIds(
        stub: T
    ): Set<String>

    fun <R> startAsync(
        invoke: () -> R
    ): CompletableFuture<Deferred<R>>

    fun startVoidAsync(
        invoke: () -> Unit
    ): CompletableFuture<Deferred<Void>>
}
