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

import io.infinitic.client.dispatcher.ClientDispatcher
import io.infinitic.client.dispatcher.ClientDispatcherImpl
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.proxies.GetTaskProxyHandler
import io.infinitic.common.proxies.GetWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.exceptions.thisShouldNotHappen
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.Closeable
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@Suppress("MemberVisibilityCanBePrivate", "unused")
abstract class InfiniticClient : Closeable {
    abstract val clientName: ClientName

    protected abstract val sendToTaskTagEngine: SendToTaskTagEngine
    protected abstract val sendToTaskEngine: SendToTaskEngine
    protected abstract val sendToWorkflowTagEngine: SendToWorkflowTagEngine
    protected abstract val sendToWorkflowEngine: SendToWorkflowEngine

    protected val logger = KotlinLogging.logger {}

    private val sendThreadPool = Executors.newCachedThreadPool()

    open val sendingScope = CoroutineScope(sendThreadPool.asCoroutineDispatcher() + Job())

    private val runningThreadPool = Executors.newCachedThreadPool()

    open val runningScope = CoroutineScope(runningThreadPool.asCoroutineDispatcher() + Job())

    val dispatcher: ClientDispatcher by lazy {
        ClientDispatcherImpl(
            sendingScope,
            clientName,
            sendToTaskTagEngine,
            sendToTaskEngine,
            sendToWorkflowTagEngine,
            sendToWorkflowEngine
        )
    }

    /**
     *  Get last Deferred created by the call of a stub
     */
    val lastDeferred get() = dispatcher.getLastDeferred()

    override fun close() {
        // first make sure that all messages are sent
        join()

        // only then, close everything
        sendingScope.cancel()
        sendThreadPool.shutdown()

        runningScope.cancel()
        runningThreadPool.shutdown()
    }

    /**
     * Wait for all coroutines dispatched in sendingScope
     */
    fun join() = runBlocking {
        sendingScope.coroutineContext.job.children.forEach { it.join() }
    }

    suspend fun handle(message: ClientMessage) {
        logger.debug { "receiving $message" }

        dispatcher.handle(message)
    }

    /**
     *  Create a stub for a new task
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
     *  Create a stub for a new workflow
     */
    @JvmOverloads
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions = WorkflowOptions(),
        meta: Map<String, ByteArray> = mapOf(),
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
    fun <T : Any> getWorkflowById(
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
    fun <T : Any> getWorkflowByTag(
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
    ): Any? = when (val handler = getProxyHandler(stub)) {
        is GetTaskProxyHandler -> when {
            handler.taskId != null ->
                dispatcher.awaitTask(
                    handler.taskName,
                    handler.taskId!!,
                    false
                )
            handler.taskTag != null ->
                TODO("Not yet implemented")
            else ->
                throw thisShouldNotHappen()
        }
        is GetWorkflowProxyHandler -> when {
            handler.workflowId != null ->
                dispatcher.awaitWorkflow(
                    handler.workflowName,
                    handler.workflowId!!,
                    MethodRunId.from(handler.workflowId!!),
                    false
                )
            handler.workflowTag != null ->
                TODO("Not yet implemented")
            else ->
                throw thisShouldNotHappen()
        }
        else -> throw InvalidStubException("$stub")
    }

    /**
     * Await a method from a running workflow targeted by its id and the methodRunId
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        methodRunId: String
    ): Any? = when (val handler = getProxyHandler(stub)) {
        is GetWorkflowProxyHandler -> when {
            handler.workflowId != null ->
                dispatcher.awaitWorkflow(
                    handler.workflowName,
                    handler.workflowId!!,
                    MethodRunId.from(methodRunId),
                    false
                )
            handler.workflowTag != null ->
                TODO("Not yet implemented")
            else ->
                throw thisShouldNotHappen()
        }
        else -> throw InvalidStubException("$stub")
    }

    /**
     * Cancel a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancelAsync(
        stub: T
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is GetTaskProxyHandler ->
            dispatcher.cancelTaskAsync(handler.taskName, handler.taskId, handler.taskTag)
        is GetWorkflowProxyHandler ->
            dispatcher.cancelWorkflowAsync(handler.workflowName, handler.workflowId, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

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
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is GetTaskProxyHandler ->
            dispatcher.completeTaskAsync(handler.taskName, handler.taskId, handler.taskTag, value)
        is GetWorkflowProxyHandler ->
            dispatcher.completeWorkflowAsync(handler.workflowName, handler.workflowId, handler.workflowTag, value)
        else ->
            throw InvalidStubException("$stub")
    }

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
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is GetTaskProxyHandler ->
            dispatcher.retryTaskAsync(handler.taskName, handler.taskId, handler.taskTag)
        is GetWorkflowProxyHandler ->
            dispatcher.retryWorkflowAsync(handler.workflowName, handler.workflowId, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry a task or a workflow
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
    ): Unit = retryAsync(stub).join()

    /**
     * get ids of a stub, associated to a specific tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getIds(
        stub: T
    ): Set<String> = when (val handler = getProxyHandler(stub)) {
        is GetTaskProxyHandler ->
            dispatcher.getTaskIdsByTag(handler.taskName, handler.taskId, handler.taskTag)
        is GetWorkflowProxyHandler ->
            dispatcher.getWorkflowIdsByTag(handler.workflowName, handler.workflowId, handler.workflowTag)
        else ->
            throw InvalidStubException("$stub")
    }

    private fun <R> startAsync(
        invoke: () -> R
    ): CompletableFuture<Deferred<R>> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatchAsync(handler)
    }

    private fun startVoidAsync(
        invoke: () -> Unit
    ): CompletableFuture<Deferred<Void>> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatchAsync(handler)
    }

    private fun getProxyHandler(stub: Any): ProxyHandler<*> {
        val exception by lazy { InvalidStubException(stub::class.java.name) }

        val handler = try {
            Proxy.getInvocationHandler(stub)
        } catch (e: IllegalArgumentException) {
            throw exception
        }

        if (handler !is ProxyHandler<*>) throw exception

        return handler
    }
}
