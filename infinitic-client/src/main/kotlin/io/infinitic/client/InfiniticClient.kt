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

import io.infinitic.client.deferred.DeferredTask
import io.infinitic.client.deferred.DeferredWorkflow
import io.infinitic.client.dispatcher.ClientDispatcher
import io.infinitic.client.dispatcher.ClientDispatcherImpl
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskInstanceProxyHandler
import io.infinitic.common.proxies.TaskSelectionProxyHandler
import io.infinitic.common.proxies.WorkflowInstanceProxyHandler
import io.infinitic.common.proxies.WorkflowSelectionProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.exceptions.clients.CanNotAwaitStubPerTag
import io.infinitic.exceptions.clients.InvalidInstanceStubException
import io.infinitic.exceptions.clients.InvalidInterfaceException
import io.infinitic.exceptions.clients.InvalidNewStubException
import io.infinitic.exceptions.thisShouldNotHappen
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
    fun <T : Any> newTaskStub(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions = TaskOptions(),
        meta: Map<String, ByteArray> = mapOf()
    ): T = TaskInstanceProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options,
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /**
     *  Create a stub for a workflow
     */
    @JvmOverloads
    fun <T : Any> newWorkflowStub(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions = WorkflowOptions(),
        meta: Map<String, ByteArray> = mapOf(),
    ): T = WorkflowInstanceProxyHandler(
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

    /**
     * Create instance stub per id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstanceStub(
        stub: T,
        id: UUID
    ): T = when (val handler = getProxyHandler(stub, "getInstanceStub")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> getTask(handler.klass as Class<T>, id)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> getWorkflow(handler.klass as Class<T>, id)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "getInstanceStub")
    }

    /**
     * Create instance stub per tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getInstanceStub(
        stub: T,
        tag: String
    ): T = when (val handler = getProxyHandler(stub, "getInstanceStub")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> getTask(handler.klass as Class<T>, tag)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> getWorkflow(handler.klass as Class<T>, tag)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "getInstanceStub")
    }

    /**
     * Await by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> await(
        stub: T,
        id: UUID
    ): Any = when (val handler = getProxyHandler(stub, "await")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> awaitTask(handler.klass as Class<T>, id)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> awaitWorkflow(handler.klass as Class<T>, id)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "await")
    }

    /**
     * Cancel by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "cancel")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> cancelTask(handler.klass as Class<T>, id)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> cancelWorkflow(handler.klass as Class<T>, id)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "cancel")
    }

    /**
     * Cancel by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> cancel(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "cancel")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> cancelTask(handler.klass as Class<T>, tag)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> cancelWorkflow(handler.klass as Class<T>, tag)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "cancel")
    }

    /**
     * Completed by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        id: UUID,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "complete")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> completeTask(handler.klass as Class<T>, id, value)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> completeWorkflow(handler.klass as Class<T>, id, value)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "complete")
    }

    /**
     * Completed by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> complete(
        stub: T,
        tag: String,
        value: Any?
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "complete")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> completeTask(handler.klass as Class<T>, tag, value)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> completeWorkflow(handler.klass as Class<T>, tag, value)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "complete")
    }

    /**
     * Retry by id
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        id: UUID
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "retry")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> retryTask(handler.klass as Class<T>, id)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> retryWorkflow(handler.klass as Class<T>, id)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "retry")
    }

    /**
     * Retry by tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> retry(
        stub: T,
        tag: String
    ): CompletableFuture<Unit> = when (val handler = getProxyHandler(stub, "retry")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> retryTask(handler.klass as Class<T>, tag)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> retryWorkflow(handler.klass as Class<T>, tag)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "retry")
    }

    /**
     * get ids of a stub, associated to a specific tag
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getIds(
        stub: T,
        tag: String
    ): Set<UUID> = when (val handler = getProxyHandler(stub, "getIds")) {
        is TaskInstanceProxyHandler, is TaskSelectionProxyHandler -> getTaskIds(handler.klass as Class<T>, tag)
        is WorkflowInstanceProxyHandler, is WorkflowSelectionProxyHandler -> getWorkflowIds(handler.klass as Class<T>, tag)
        is ChannelProxyHandler -> throw InvalidInstanceStubException(handler.klass.name, "getIds")
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

    private fun getProxyHandler(stub: Any, action: String): ProxyHandler<*> {
        val exception by lazy { InvalidNewStubException(stub::class.java.name, action) }

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

    private fun <R : Any?> await(stub: Any): R =
        when (val handler = getProxyHandler(stub, "await")) {
            is TaskInstanceProxyHandler -> thisShouldNotHappen()
            is WorkflowInstanceProxyHandler -> thisShouldNotHappen()
            is TaskSelectionProxyHandler -> when {
                handler.perTaskId != null -> DeferredTask<R>(handler.selection(), false, dispatcher).await()
                handler.perTaskTag != null -> throw CanNotAwaitStubPerTag("${handler.taskName}")
                else -> thisShouldNotHappen()
            }
            is WorkflowSelectionProxyHandler -> when {
                handler.perWorkflowId != null -> DeferredWorkflow<R>(handler.selection(), false, dispatcher).await()
                handler.perWorkflowTag != null -> throw CanNotAwaitStubPerTag("${handler.workflowName}")
                else -> thisShouldNotHappen()
            }
            is ChannelProxyHandler -> thisShouldNotHappen()
        }

    private fun retry(stub: Any): CompletableFuture<Unit> =
        when (val handler = getProxyHandler(stub, "retry")) {
            is TaskInstanceProxyHandler -> thisShouldNotHappen()
            is WorkflowInstanceProxyHandler -> thisShouldNotHappen()
            is TaskSelectionProxyHandler<*> -> dispatcher.retryTask(handler.selection())
            is WorkflowSelectionProxyHandler<*> -> dispatcher.retryWorkflow(handler.selection())
            is ChannelProxyHandler<*> -> throw InvalidInstanceStubException(handler.klass.name, "retry")
        }

    private fun cancel(stub: Any): CompletableFuture<Unit> =
        when (val handler = getProxyHandler(stub, "cancel")) {
            is TaskInstanceProxyHandler -> thisShouldNotHappen()
            is WorkflowInstanceProxyHandler -> thisShouldNotHappen()
            is TaskSelectionProxyHandler<*> -> dispatcher.cancelTask(handler.selection())
            is WorkflowSelectionProxyHandler<*> -> dispatcher.cancelWorkflow(handler.selection())
            is ChannelProxyHandler<*> -> throw InvalidInstanceStubException(handler.klass.name, "cancel")
        }

    private fun complete(stub: Any, value: Any?): CompletableFuture<Unit> =
        when (val handler = getProxyHandler(stub, "complete")) {
            is TaskInstanceProxyHandler -> thisShouldNotHappen()
            is WorkflowInstanceProxyHandler -> thisShouldNotHappen()
            is TaskSelectionProxyHandler<*> -> TODO("Not yet implemented")
            is WorkflowSelectionProxyHandler<*> -> TODO("Not yet implemented")
            is ChannelProxyHandler<*> -> throw InvalidInstanceStubException(handler.klass.name, "complete")
        }

    /**
     * Create stub for an existing task targeted per id
     */
    private fun <T : Any> getTask(
        klass: Class<out T>,
        id: UUID
    ): T = TaskSelectionProxyHandler(
        klass = klass,
        perTaskId = TaskId(id)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per tag
     */
    private fun <T : Any> getTask(
        klass: Class<out T>,
        tag: String
    ): T = TaskSelectionProxyHandler(
        klass = klass,
        perTaskTag = TaskTag(tag)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per id
     */
    private fun <T : Any> getWorkflow(
        klass: Class<out T>,
        id: UUID
    ): T = WorkflowSelectionProxyHandler(
        klass = klass,
        perWorkflowId = WorkflowId(id)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per tag
     */
    private fun <T : Any> getWorkflow(
        klass: Class<out T>,
        tag: String
    ): T = WorkflowSelectionProxyHandler(
        klass = klass,
        perWorkflowTag = WorkflowTag(tag)
    ) { dispatcher }.stub()

    /**
     * Await a task by id
     */
    private fun <T : Any> awaitTask(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getTask(klass, id))

    /**
     * Await a workflow by id
     */
    private fun <T : Any> awaitWorkflow(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getWorkflow(klass, id))

    /**
     *  Cancel a task by id
     */
    private fun <T : Any> cancelTask(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getTask(klass, id))

    /**
     *  Cancel a workflow by id
     */
    private fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getWorkflow(klass, id))

    /**
     *  Cancel a task by tag
     */
    private fun <T : Any> cancelTask(
        klass: Class<out T>,
        tag: String
    ) = cancel(getTask(klass, tag))

    /**
     *  Cancel a workflow by tag
     */
    private fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        tag: String
    ) = cancel(getWorkflow(klass, tag))

    /**
     *  Complete a task by id
     */
    private fun <T : Any> completeTask(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getTask(klass, id), value)

    /**
     *  Complete a workflow by id
     */
    private fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getWorkflow(klass, id), value)

    /**
     *  Complete a task by tag
     */
    private fun <T : Any> completeTask(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getTask(klass, tag), value)

    /**
     *  Complete a workflow by tag
     */
    private fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getWorkflow(klass, tag), value)

    /**
     * Retry a task by id
     */
    private fun <T : Any> retryTask(
        klass: Class<out T>,
        id: UUID
    ) = retry(getTask(klass, id))

    /**
     * Retry a workflow by id
     */
    private fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = retry(getWorkflow(klass, id))

    /**
     * Retry a task by tag
     */
    private fun <T : Any> retryTask(
        klass: Class<out T>,
        tag: String
    ) = retry(getTask(klass, tag))

    /**
     * Retry a workflow by tag
     */
    private fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        tag: String
    ) = retry(getWorkflow(klass, tag))

    /**
     * Synchronous call to get task'ids per tag and name
     */
    private fun <T : Any> getTaskIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getTaskIdsPerTag(
        TaskName(klass.name),
        TaskTag(tag)
    )

    /**
     * Synchronous call to get WorkflowIds per tag and name
     */
    private fun <T : Any> getWorkflowIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getWorkflowIdsPerTag(
        WorkflowName(klass.name),
        WorkflowTag(tag)
    )
}
