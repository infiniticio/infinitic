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
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.exceptions.clients.InvalidChannelException
import io.infinitic.exceptions.clients.InvalidStubException
import io.infinitic.exceptions.clients.InvalidWorkflowException
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import org.jetbrains.annotations.TestOnly
import java.io.Closeable
import java.lang.reflect.Proxy
import java.util.UUID
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
     *  Get last Deferred created by the synchronous call of a stub
     */
    @TestOnly
    fun getLastSyncDeferred() = dispatcher.getLastSyncDeferred()

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
     *  Start a method without parameter for a running workflow targeted by id
     */
    fun <R : Any?> dispatch(
        method: Function0<R>,
        id: UUID
    ): With0<R> = With0 {
        dispatch(id) { method.invoke() }
    }

    /**
     *  Start a method with 1 parameter for a running workflow targeted by id
     */
    fun <P1, R : Any?> dispatch(
        method: Function1<P1, R>,
        id: UUID
    ): With1<P1, R> = With1 { p1 ->
        dispatch(id) { method.invoke(p1) }
    }

    /**
     *  Start a method with 2 parameters for a running workflow targeted by id
     */
    fun <P1, P2, R : Any?> dispatch(
        method: Function2<P1, P2, R>,
        id: UUID
    ): With2<P1, P2, R> = With2 { p1, p2 ->
        dispatch(id) { method.invoke(p1, p2) }
    }

    /**
     *  Start a method with 3 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, R : Any?> dispatch(
        method: Function3<P1, P2, P3, R>,
        id: UUID
    ): With3<P1, P2, P3, R> = With3 { p1, p2, p3 ->
        dispatch(id) { method.invoke(p1, p2, p3) }
    }

    /**
     *  Start a method with 4 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, R : Any?> dispatch(
        method: Function4<P1, P2, P3, P4, R>,
        id: UUID
    ): With4<P1, P2, P3, P4, R> = With4 { p1, p2, p3, p4 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4) }
    }

    /**
     *  Start a method with 5 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, P5, R : Any?> dispatch(
        method: Function5<P1, P2, P3, P4, P5, R>,
        id: UUID
    ): With5<P1, P2, P3, P4, P5, R> = With5 { p1, p2, p3, p4, p5 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4, p5) }
    }

    /**
     *  Start a method with 6 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, P5, P6, R : Any?> dispatch(
        method: Function6<P1, P2, P3, P4, P5, P6, R>,
        id: UUID
    ): With6<P1, P2, P3, P4, P5, P6, R> = With6 { p1, p2, p3, p4, p5, p6 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4, p5, p6) }
    }

    /**
     *  Start a method with 7 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, P5, P6, P7, R : Any?> dispatch(
        method: Function7<P1, P2, P3, P4, P5, P6, P7, R>,
        id: UUID
    ): With7<P1, P2, P3, P4, P5, P6, P7, R> = With7 { p1, p2, p3, p4, p5, p6, p7 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4, p5, p6, p7) }
    }

    /**
     *  Start a method with 8 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, R : Any?> dispatch(
        method: Function8<P1, P2, P3, P4, P5, P6, P7, P8, R>,
        id: UUID
    ): With8<P1, P2, P3, P4, P5, P6, P7, P8, R> = With8 { p1, p2, p3, p4, p5, p6, p7, p8 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8) }
    }

    /**
     *  Start a method with 8 parameters for a running workflow targeted by id
     */
    fun <P1, P2, P3, P4, P5, P6, P7, P8, P9, R : Any?> dispatch(
        method: Function9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R>,
        id: UUID
    ): With9<P1, P2, P3, P4, P5, P6, P7, P8, P9, R> = With9 { p1, p2, p3, p4, p5, p6, p7, p8, p9 ->
        dispatch(id) { method.invoke(p1, p2, p3, p4, p5, p6, p7, p8, p9) }
    }

    /**
     * Send a signal in a channel of a workflow targeted by its id
     */
    fun <S : Any> send(
        stub: SendChannel<S>,
        id: UUID,
        signal: S
    ): Deferred<S> = when (val handler = getProxyHandler(stub)) {
        is ChannelProxyHandler ->
            dispatcher.send(
                handler.workflowName,
                handler.channelName,
                workflowId = WorkflowId(id),
                signal
            )
        else -> throw InvalidChannelException("$stub")
    }

    /**
     * Send a signal in a channel of workflows targeted by tag
     */
    fun <S : Any> send(
        stub: SendChannel<S>,
        tag: String,
        signal: S
    ): Deferred<S> = when (val handler = getProxyHandler(stub)) {
        is ChannelProxyHandler ->
            dispatcher.send(
                handler.workflowName,
                handler.channelName,
                workflowTag = WorkflowTag(tag),
                signal
            )
        else -> throw InvalidChannelException("$stub")
    }

    /**
     * Await a task or a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        workflowId: UUID
    ): Any = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler -> dispatcher.awaitTask<Any>(
            handler.taskName,
            TaskId(workflowId),
            false
        )
        is WorkflowProxyHandler -> dispatcher.awaitWorkflow(
            handler.workflowName,
            WorkflowId(workflowId),
            MethodRunId(workflowId),
            false
        )
        is ChannelProxyHandler -> throw InvalidStubException("$stub")
    }

    /**
     * Await a method from a running workflow targeted by its id and the methodRunId
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        workflowId: UUID,
        methodRunId: UUID
    ): Any = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler -> throw InvalidWorkflowException("$stub")
        is WorkflowProxyHandler -> dispatcher.awaitWorkflow<Any>(
            handler.workflowName,
            WorkflowId(workflowId),
            MethodRunId(methodRunId),
            false
        )
        is ChannelProxyHandler -> throw InvalidStubException("$stub")
    }

    /**
     * Cancel a task or a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.cancelTask(handler.taskName, taskId = TaskId(id))
        is WorkflowProxyHandler ->
            dispatcher.cancelWorkflow(handler.workflowName, workflowId = WorkflowId(id))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Cancel tasks or workflows targeted per tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.cancelTask(handler.taskName, taskTag = TaskTag(tag))
        is WorkflowProxyHandler ->
            dispatcher.cancelWorkflow(handler.workflowName, workflowTag = WorkflowTag(tag))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Complete a task or a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        id: UUID,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.completeTask(handler.taskName, taskId = TaskId(id), value)
        is WorkflowProxyHandler ->
            dispatcher.completeWorkflow(handler.workflowName, workflowId = WorkflowId(id), value)
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Complete tasks or workflows targeted per tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        tag: String,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.completeTask(handler.taskName, taskTag = TaskTag(tag), value)
        is WorkflowProxyHandler ->
            dispatcher.completeWorkflow(handler.workflowName, workflowTag = WorkflowTag(tag), value)
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry a task or a workflow targeted by its id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.retryTask(handler.taskName, perTaskId = TaskId(id))
        is WorkflowProxyHandler ->
            dispatcher.retryWorkflow(handler.workflowName, workflowId = WorkflowId(id))
        is ChannelProxyHandler ->
            throw InvalidStubException("$stub")
    }

    /**
     * Retry tasks or workflows targeted per tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub)) {
        is TaskProxyHandler ->
            dispatcher.retryTask(handler.taskName, perTaskTag = TaskTag(tag))
        is WorkflowProxyHandler ->
            dispatcher.retryWorkflow(handler.workflowName, workflowTag = WorkflowTag(tag))
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
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, false, tags, options, meta)
    }

    private fun <R> dispatch(
        id: UUID,
        invoke: () -> R
    ): Deferred<R> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, id)
    }

    private fun <R> dispatch(
        tag: String,
        invoke: () -> R
    ): Deferred<R> {
        val handler = ProxyHandler.async(invoke) ?: throw InvalidStubException()

        return dispatcher.dispatch(handler, tag)
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
