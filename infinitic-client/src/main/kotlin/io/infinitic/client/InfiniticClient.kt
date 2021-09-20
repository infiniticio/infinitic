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
import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.exceptions.clients.InvalidChannelException
import io.infinitic.exceptions.clients.InvalidInterfaceException
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import java.io.Closeable
import java.lang.reflect.Proxy
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
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
import kotlin.reflect.full.extensionReceiverParameter
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.jvm.javaMethod

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

    private val dispatcher: ClientDispatcher by lazy {
        ClientDispatcherImpl(
            sendingScope,
            clientName,
            sendToTaskTagEngine,
            sendToTaskEngine,
            sendToWorkflowTagEngine,
            sendToWorkflowEngine
        )
    }

    override fun close() {
        // first make sure that all messages are sent
        join()

        // only then, close everything
        sendingScope.cancel()
        sendThreadPool.shutdown()

        runningScope.cancel()
        runningThreadPool.shutdown()
    }

    fun join() = runBlocking {
        sendingScope.coroutineContext.job.children.forEach { it.join() }
    }

    suspend fun handle(message: ClientMessage) {
        logger.debug { "receiving $message" }

        dispatcher.handle(message)
    }

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
        meta: Map<String, ByteArray> = mapOf(),
    ): T = WorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options,
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /**
     *  Dispatch a task or workflow without parameter
     */
    @JvmOverloads
    fun <R : Any?> dispatch(
        method: KFunction0<R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): () -> Deferred<R> = {
        dispatch(tags, options, meta) {
            method.check().call()
        }
    }

    /**
     *  Dispatch a task or workflow with 1 parameter
     */
    @JvmOverloads
    fun <P1, R : Any?> dispatch(
        method: KFunction1<P1, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1) -> Deferred<R> = { p1: P1 ->
        dispatch(tags, options, meta) {
            method.check().call(p1)
        }
    }

    /**
     *  Dispatch a task or workflow with 2 parameters
     */
    @JvmOverloads
    fun <P1, P2, R : Any?> dispatch(
        method: KFunction2<P1, P2, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2) -> Deferred<R> = { p1: P1, p2: P2 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2)
        }
    }

    /**
     *  Dispatch a task or workflow with 3 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, R : Any?> dispatch(
        method: KFunction3<P1, P2, P3, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3) -> Deferred<R> = { p1: P1, p2: P2, p3: P3 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3)
        }
    }

    /**
     *  Dispatch a task or workflow with 4 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: KFunction4<P1, P2, P3, P4, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4)
        }
    }

    /**
     *  Dispatch a task or workflow with 5 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
        method: KFunction5<P1, P2, P3, P4, P5, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4, P5) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4, p5)
        }
    }

    /**
     *  Dispatch a task or workflow with 6 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
        method: KFunction6<P1, P2, P3, P4, P5, P6, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4, P5, P6) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4, p5, p6)
        }
    }

    /**
     *  Dispatch a task or workflow with 7 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
        method: KFunction7<P1, P2, P3, P4, P5, P6, P7, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4, P5, P6, P7) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4, p5, p6, p7)
        }
    }

    /**
     *  Dispatch a task or workflow with 8 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
        method: KFunction8<P1, P2, P3, P4, P5, P6, P7, P8, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4, P5, P6, P7, P8) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4, p5, p6, p7, p8)
        }
    }

    /**
     *  Dispatch a task or workflow with 9 parameters
     */
    @JvmOverloads
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
        method: KFunction9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>,
        tags: Set<String>? = null,
        options: JobOptions? = null,
        meta: Map<String, ByteArray>? = null
    ): (P1, P2, P3, P4, P5, P6, P7, P8, P9) -> Deferred<R> = { p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8, p9: P9 ->
        dispatch(tags, options, meta) {
            method.check().call(p1, p2, p3, p4, p5, p6, p7, p8, p9)
        }
    }

    fun <S : Any> send(
        stub: SendChannel<S>,
        id: UUID,
    ): (S) -> Deferred<S> = when (val handler = getProxyHandler(stub)) {
        is ChannelProxyHandler -> { signal: S ->
            dispatcher.send(
                handler.workflowName,
                handler.channelName,
                perWorkflowId = WorkflowId(id),
                signal
            )
        }
        else -> throw InvalidChannelException("$stub")
    }

    fun <S : Any> send(
        stub: SendChannel<S>,
        tag: String,
    ): (S) -> Deferred<S> = when (val handler = getProxyHandler(stub)) {
        is ChannelProxyHandler -> { signal: S ->
            dispatcher.send(
                handler.workflowName,
                handler.channelName,
                perWorkflowTag = WorkflowTag(tag),
                signal
            )
        }
        else -> throw InvalidChannelException("$stub")
    }

    /**
     * Await by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        id: UUID
    ): Any = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.awaitTask<Any>(handler.taskName, perTaskId = TaskId(id), false)
        is WorkflowProxyHandler ->
            dispatcher.awaitWorkflow<Any>(handler.workflowName, perWorkflowId = WorkflowId(id), false)
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Cancel by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.cancelTask(handler.taskName, perTaskId = TaskId(id))
        is WorkflowProxyHandler ->
            dispatcher.cancelWorkflow(handler.workflowName, perWorkflowId = WorkflowId(id))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Cancel by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.cancelTask(handler.taskName, perTaskTag = TaskTag(tag))
        is WorkflowProxyHandler ->
            dispatcher.cancelWorkflow(handler.workflowName, perWorkflowTag = WorkflowTag(tag))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Completed by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        id: UUID,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.completeTask(handler.taskName, perTaskId = TaskId(id), value)
        is WorkflowProxyHandler ->
            dispatcher.completeWorkflow(handler.workflowName, perWorkflowId = WorkflowId(id), value)
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Completed by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        tag: String,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.completeTask(handler.taskName, perTaskTag = TaskTag(tag), value)
        is WorkflowProxyHandler ->
            dispatcher.completeWorkflow(handler.workflowName, perWorkflowTag = WorkflowTag(tag), value)
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.retryTask(handler.taskName, perTaskId = TaskId(id))
        is WorkflowProxyHandler ->
            dispatcher.retryWorkflow(handler.workflowName, perWorkflowId = WorkflowId(id))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.retryTask(handler.taskName, perTaskTag = TaskTag(tag))
        is WorkflowProxyHandler ->
            dispatcher.retryWorkflow(handler.workflowName, perWorkflowTag = WorkflowTag(tag))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * get ids of a stub, associated to a specific tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getIds(
        stub: T,
        tag: String
    ): Set<UUID> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.getTaskIdsPerTag(handler.taskName, TaskTag(tag))
        is WorkflowProxyHandler ->
            dispatcher.getWorkflowIdsPerTag(handler.workflowName, WorkflowTag(tag))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    private fun <R> dispatch(
        tags: Set<String>?,
        options: JobOptions?,
        meta: Map<String, ByteArray>?,
        invoke: () -> R
    ): Deferred<R> {
        val handler = ProxyHandler.async(invoke) ?: throw thisShouldNotHappen("should be called through a stub")

        return dispatcher.dispatch(handler, false, tags, options, meta)
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

    private fun <R> KFunction<R>.check(): KFunction<R> = this.also {
        if (javaMethod?.declaringClass?.isInterface != true || (instanceParameter ?: extensionReceiverParameter) != null)
            throw InvalidInterfaceException(name, "dispatch")
    }
}
