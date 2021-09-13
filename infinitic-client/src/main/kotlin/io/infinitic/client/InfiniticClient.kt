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

import io.infinitic.client.proxies.ClientDispatcher
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RunningProxyHandler
import io.infinitic.common.proxies.RunningTaskProxyHandler
import io.infinitic.common.proxies.RunningWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
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
import io.infinitic.exceptions.clients.CanNotApplyOnChannelException
import io.infinitic.exceptions.clients.CanNotApplyOnNewTaskStubException
import io.infinitic.exceptions.clients.CanNotApplyOnNewWorkflowStubException
import io.infinitic.exceptions.clients.NotAStubException
import io.infinitic.exceptions.clients.NotAnInterfaceException
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
import kotlin.reflect.KCallable
import kotlin.reflect.KFunction
import kotlin.reflect.KFunction0
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction10
import kotlin.reflect.KFunction2
import kotlin.reflect.KFunction3
import kotlin.reflect.KFunction4
import kotlin.reflect.KFunction5
import kotlin.reflect.KFunction6
import kotlin.reflect.KFunction7
import kotlin.reflect.KFunction8
import kotlin.reflect.KFunction9
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

    private val dispatcher by lazy {
        ClientDispatcher(
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
     * Create stub for a new task
     */
    @JvmOverloads
    fun <T : Any> newTask(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewTaskProxyHandler(
        klass = klass,
        taskTags = tags.map { TaskTag(it) }.toSet(),
        taskOptions = options ?: TaskOptions(),
        taskMeta = TaskMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per id
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        id: UUID
    ): T = RunningTaskProxyHandler(
        klass = klass,
        perTaskId = TaskId(id)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing task targeted per tag
     */
    fun <T : Any> getTask(
        klass: Class<out T>,
        tag: String
    ): T = RunningTaskProxyHandler(
        klass = klass,
        perTag = TaskTag(tag)
    ) { dispatcher }.stub()

    /**
     * Synchronous call to get task'ids per tag and name
     */
    fun <T : Any> getTaskIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getTaskIdsPerTag(
        TaskName(klass.name),
        TaskTag(tag)
    )

    /**
     * Create stub for a new workflow
     */
    @JvmOverloads
    fun <T : Any> newWorkflow(
        klass: Class<out T>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): T = NewWorkflowProxyHandler(
        klass = klass,
        workflowTags = tags.map { WorkflowTag(it) }.toSet(),
        workflowOptions = options ?: WorkflowOptions(),
        workflowMeta = WorkflowMeta(meta)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per id
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        id: UUID
    ): T = RunningWorkflowProxyHandler(
        klass = klass,
        perWorkflowId = WorkflowId(id)
    ) { dispatcher }.stub()

    /**
     * Create stub for an existing workflow per tag
     */
    fun <T : Any> getWorkflow(
        klass: Class<out T>,
        tag: String
    ): T = RunningWorkflowProxyHandler(
        klass = klass,
        perTag = WorkflowTag(tag)
    ) { dispatcher }.stub()

    /**
     * Synchronous call to get WorkflowIds per tag and name
     */
    fun <T : Any> getWorkflowIds(
        klass: Class<out T>,
        tag: String
    ): Set<UUID> = dispatcher.getWorkflowIdsPerTag(
        WorkflowName(klass.name),
        WorkflowTag(tag)
    )

    /**
     *  Asynchronously process a task or a workflow
     */

    fun <R : Any?> dispatch(method: KFunction0<R>): () -> Deferred<R> =
        { run { method.check().call() } }

    fun <S1, R : Any?> dispatch(method: KFunction1<S1, R>): (S1) -> Deferred<R> =
        { s: S1 -> run { method.check().call(s) } }

    fun <S1, S2, R : Any?> dispatch(method: KFunction2<S1, S2, R>): (S1, S2) -> Deferred<R> =
        { s1: S1, s2: S2 -> run { method.check().call(s1, s2) } }

    fun <S1, S2, S3, R : Any?> dispatch(method: KFunction3<S1, S2, S3, R>): (S1, S2, S3) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3 -> run { method.check().call(s1, s2, s3) } }

    fun <S1, S2, S3, S4, R : Any?> dispatch(method: KFunction4<S1, S2, S3, S4, R>): (S1, S2, S3, S4) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4 -> run { method.check().call(s1, s2, s3, s4) } }

    fun <S1, S2, S3, S4, S5, R : Any?> dispatch(method: KFunction5<S1, S2, S3, S4, S5, R>): (S1, S2, S3, S4, S5) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5 -> run { method.check().call(s1, s2, s3, s4, s5) } }

    fun <S1, S2, S3, S4, S5, S6, R : Any?> dispatch(method: KFunction6<S1, S2, S3, S4, S5, S6, R>): (S1, S2, S3, S4, S5, S6) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6 -> run { method.check().call(s1, s2, s3, s4, s5, s6) } }

    fun <S1, S2, S3, S4, S5, S6, S7, R : Any?> dispatch(method: KFunction7<S1, S2, S3, S4, S5, S6, S7, R>): (S1, S2, S3, S4, S5, S6, S7) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7 -> run { method.check().call(s1, s2, s3, s4, s5, s6, s7) } }

    fun <S1, S2, S3, S4, S5, S6, S7, S8, R : Any?> dispatch(method: KFunction8<S1, S2, S3, S4, S5, S6, S7, S8, R>): (S1, S2, S3, S4, S5, S6, S7, S8) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8 -> run { method.check().call(s1, s2, s3, s4, s5, s6, s7, s8) } }

    fun <S1, S2, S3, S4, S5, S6, S7, S8, S9, R : Any?> dispatch(method: KFunction9<S1, S2, S3, S4, S5, S6, S7, S8, S9, R>): (S1, S2, S3, S4, S5, S6, S7, S8, S9) -> Deferred<R> =
        { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8, s9: S9 -> run { method.check().call(s1, s2, s3, s4, s5, s6, s7, s8, s9) } }

    /**
     *  Asynchronously process a task (helper)
     */
    @JvmOverloads
    fun <K : Any, R : Any?> dispatchTask(
        method: KFunction1<K, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): () -> Deferred<R> = {
        run {
            method.checkTask<K, R>(tags, options, meta).call()
        }
    }

    @JvmOverloads
    fun <K : Any, S1, R : Any?> dispatchTask(
        method: KFunction2<K, S1, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1) -> Deferred<R> = { s1: S1 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, R : Any?> dispatchTask(
        method: KFunction3<K, S1, S2, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2) -> Deferred<R> = { s1: S1, s2: S2 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, R : Any?> dispatchTask(
        method: KFunction4<K, S1, S2, S3, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3) -> Deferred<R> = { s1: S1, s2: S2, s3: S3 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, R : Any?> dispatchTask(
        method: KFunction5<K, S1, S2, S3, S4, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, R : Any?> dispatchTask(
        method: KFunction6<K, S1, S2, S3, S4, S5, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, R : Any?> dispatchTask(
        method: KFunction7<K, S1, S2, S3, S4, S5, S6, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, R : Any?> dispatchTask(
        method: KFunction8<K, S1, S2, S3, S4, S5, S6, S7, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, S8, R : Any?> dispatchTask(
        method: KFunction9<K, S1, S2, S3, S4, S5, S6, S7, S8, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7, S8) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7, s8)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, S8, S9, R : Any?> dispatchTask(
        method: KFunction10<K, S1, S2, S3, S4, S5, S6, S7, S8, S9, R>,
        tags: Set<String> = setOf(),
        options: TaskOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7, S8, S9) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8, s9: S9 ->
        run {
            method.checkTask<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7, s8, s9)
        }
    }

    /**
     *  Asynchronously process a workflow (helper)
     */
    @JvmOverloads
    fun <K : Any, R : Any?> dispatchWorkflow(
        method: KFunction1<K, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): () -> Deferred<R> = {
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call()
        }
    }

    @JvmOverloads
    fun <K : Any, S1, R : Any?> dispatchWorkflow(
        method: KFunction2<K, S1, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1) -> Deferred<R> = { s1: S1 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, R : Any?> dispatchWorkflow(
        method: KFunction3<K, S1, S2, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2) -> Deferred<R> = { s1: S1, s2: S2 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, R : Any?> dispatchWorkflow(
        method: KFunction4<K, S1, S2, S3, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3) -> Deferred<R> = { s1: S1, s2: S2, s3: S3 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, R : Any?> dispatchWorkflow(
        method: KFunction5<K, S1, S2, S3, S4, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, R : Any?> dispatchWorkflow(
        method: KFunction6<K, S1, S2, S3, S4, S5, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, R : Any?> dispatchWorkflow(
        method: KFunction7<K, S1, S2, S3, S4, S5, S6, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, R : Any?> dispatchWorkflow(
        method: KFunction8<K, S1, S2, S3, S4, S5, S6, S7, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, S8, R : Any?> dispatchWorkflow(
        method: KFunction9<K, S1, S2, S3, S4, S5, S6, S7, S8, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7, S8) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7, s8)
        }
    }

    @JvmOverloads
    fun <K : Any, S1, S2, S3, S4, S5, S6, S7, S8, S9, R : Any?> dispatchWorkflow(
        method: KFunction10<K, S1, S2, S3, S4, S5, S6, S7, S8, S9, R>,
        tags: Set<String> = setOf(),
        options: WorkflowOptions? = null,
        meta: Map<String, ByteArray> = mapOf()
    ): (S1, S2, S3, S4, S5, S6, S7, S8, S9) -> Deferred<R> = { s1: S1, s2: S2, s3: S3, s4: S4, s5: S5, s6: S6, s7: S7, s8: S8, s9: S9 ->
        run {
            method.checkWorkflow<K, R>(tags, options, meta).call(s1, s2, s3, s4, s5, s6, s7, s8, s9)
        }
    }

    /**
     *  Cancel a task or a workflow from a stub
     */
    fun <T : Any> cancel(proxy: T): CompletableFuture<Unit> = when (Proxy.isProxyClass(proxy::class.java)) {
        false -> throw NotAStubException(proxy::class.java.name, "cancel")
        true -> when (val handler = Proxy.getInvocationHandler(proxy) as ProxyHandler<*>) {
            is NewTaskProxyHandler -> throw CanNotApplyOnNewTaskStubException(handler.klass.name, "cancel")
            is NewWorkflowProxyHandler -> throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "cancel")
            is RunningProxyHandler -> when (handler) {
                is RunningTaskProxyHandler<*> -> dispatcher.cancelTask(handler)
                is RunningWorkflowProxyHandler<*> -> dispatcher.cancelWorkflow(handler)
                is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("cancel")
            }
        }
    }

    /**
     *  Cancel a task by id
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getTask(klass, id))

    /**
     *  Cancel a task by tag
     */
    fun <T : Any> cancelTask(
        klass: Class<out T>,
        tag: String
    ) = cancel(getTask(klass, tag))

    /**
     *  Cancel a workflow by id
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = cancel(getWorkflow(klass, id))

    /**
     *  Cancel a workflow by tag
     */
    fun <T : Any> cancelWorkflow(
        klass: Class<out T>,
        tag: String
    ) = cancel(getWorkflow(klass, tag))

    /**
     * Await a task or a workflowTask from a stub
     */
    fun <R : Any?> await(proxy: Any): R = when (Proxy.isProxyClass(proxy::class.java)) {
        false -> throw NotAStubException(proxy::class.java.name, "await")
        true -> when (val handler = Proxy.getInvocationHandler(proxy) as ProxyHandler<*>) {
            is NewTaskProxyHandler -> throw CanNotApplyOnNewTaskStubException(handler.klass.name, "await")
            is NewWorkflowProxyHandler -> throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "await")
            is RunningProxyHandler -> when (handler) {
                is RunningTaskProxyHandler -> dispatcher.awaitTask<R>(handler)
                is RunningWorkflowProxyHandler -> dispatcher.awaitWorkflow(handler)
                is SendChannelProxyHandler -> throw CanNotApplyOnChannelException("await")
                else -> throw Exception()
            }
        }
    }

    /**
     * Await a task by id
     */
    fun <T : Any> awaitTask(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getTask(klass, id))

    /**
     * Await a workflow by id
     */
    fun <T : Any> awaitWorkflow(
        klass: Class<out T>,
        id: UUID
    ): Any = await(getWorkflow(klass, id))

    /**
     *  Complete a task or a workflow from a stub
     */
    fun complete(proxy: Any, value: Any?): Unit = when (Proxy.isProxyClass(proxy::class.java)) {
        false -> throw NotAStubException(proxy::class.java.name, "complete")
        true -> when (val handler = Proxy.getInvocationHandler(proxy) as ProxyHandler<*>) {
            is NewTaskProxyHandler -> throw CanNotApplyOnNewTaskStubException(handler.klass.name, "complete")
            is NewWorkflowProxyHandler -> throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "complete")
            is RunningProxyHandler -> when (handler) {
                is RunningTaskProxyHandler<*> -> TODO("Not yet implemented")
                is RunningWorkflowProxyHandler<*> -> TODO("Not yet implemented")
                is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("complete")
            }
        }
    }

    /**
     *  Complete a task by id
     */
    fun <T : Any> completeTask(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getTask(klass, id), value)

    /**
     *  Complete a task by tag
     */
    fun <T : Any> completeTask(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getTask(klass, tag), value)

    /**
     *  Complete a workflow by id
     */
    fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        id: UUID,
        value: Any?
    ) = complete(getWorkflow(klass, id), value)

    /**
     *  Complete a workflow by tag
     */
    fun <T : Any> completeWorkflow(
        klass: Class<out T>,
        tag: String,
        value: Any?
    ) = complete(getWorkflow(klass, tag), value)

    /**
     * Retry a task or a workflowTask from a stub
     */
    fun <T : Any> retry(proxy: T): CompletableFuture<Unit> {
        if (proxy !is Proxy) throw NotAStubException(proxy::class.java.name, "retry")

        return when (val handler = Proxy.getInvocationHandler(proxy) as ProxyHandler<*>) {
            is NewTaskProxyHandler -> throw CanNotApplyOnNewTaskStubException(handler.klass.name, "retry")
            is NewWorkflowProxyHandler -> throw CanNotApplyOnNewWorkflowStubException(handler.klass.name, "retry")
            is RunningProxyHandler -> when (handler) {
                is RunningTaskProxyHandler<*> -> dispatcher.retryTask(handler)
                is RunningWorkflowProxyHandler<*> -> dispatcher.retryWorkflow(handler)
                is SendChannelProxyHandler<*> -> throw CanNotApplyOnChannelException("retry")
            }
        }
    }

    /**
     * Retry a task by id
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        id: UUID
    ) = retry(getTask(klass, id))

    /**
     * Retry a task by tag
     */
    fun <T : Any> retryTask(
        klass: Class<out T>,
        tag: String
    ) = retry(getTask(klass, tag))

    /**
     * Retry a workflow by id
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        id: UUID
    ) = retry(getWorkflow(klass, id))

    /**
     * Retry a workflow by tag
     */
    fun <T : Any> retryWorkflow(
        klass: Class<out T>,
        tag: String
    ) = retry(getWorkflow(klass, tag))

    private fun <R> run(invoke: () -> R): Deferred<R> = dispatcher.dispatch(ProxyHandler.async(invoke), false)

    private fun <R> KFunction<R>.check(): KFunction<R> = this.also {
        if (javaMethod?.declaringClass?.isInterface != true) throw NotAnInterfaceException(name, "dispatch")
    }

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any, R> KFunction<R>.checkTask(
        tags: Set<String>,
        options: TaskOptions?,
        meta: Map<String, ByteArray>
    ): KCallable<R> = check().withInstance(newTask(javaMethod?.declaringClass as Class<out K>, tags, options, meta))

    @Suppress("UNCHECKED_CAST")
    private fun <K : Any, R> KFunction<R>.checkWorkflow(
        tags: Set<String>,
        options: WorkflowOptions?,
        meta: Map<String, ByteArray>
    ): KCallable<R> = check().withInstance(newWorkflow(javaMethod?.declaringClass as Class<out K>, tags, options, meta))
}
