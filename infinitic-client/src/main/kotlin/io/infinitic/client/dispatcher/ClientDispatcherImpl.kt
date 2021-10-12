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

package io.infinitic.client.dispatcher

import io.infinitic.client.Deferred
import io.infinitic.client.deferred.DeferredChannel
import io.infinitic.client.deferred.DeferredMethod
import io.infinitic.client.deferred.DeferredSend
import io.infinitic.client.deferred.DeferredTask
import io.infinitic.client.deferred.DeferredWorkflow
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.CanceledMethod
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.CompletedMethod
import io.infinitic.common.clients.messages.FailedMethod
import io.infinitic.common.clients.messages.TaskCanceled
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskFailed
import io.infinitic.common.clients.messages.TaskIdsPerTag
import io.infinitic.common.clients.messages.TaskUnknown
import io.infinitic.common.clients.messages.UnknownMethod
import io.infinitic.common.clients.messages.WorkflowIdsPerTag
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.GetTaskProxyHandler
import io.infinitic.common.proxies.GetWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.AddTagToTask
import io.infinitic.common.tasks.tags.messages.CancelTaskByTag
import io.infinitic.common.tasks.tags.messages.GetTaskIdsByTag
import io.infinitic.common.tasks.tags.messages.RetryTaskByTag
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethod
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendSignal
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.AddTagToWorkflow
import io.infinitic.common.workflows.tags.messages.CancelWorkflowByTag
import io.infinitic.common.workflows.tags.messages.DispatchMethodByTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIdsByTag
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskByTag
import io.infinitic.common.workflows.tags.messages.SendSignalByTag
import io.infinitic.exceptions.clients.CanceledException
import io.infinitic.exceptions.clients.FailedException
import io.infinitic.exceptions.clients.InvalidChannelUsageException
import io.infinitic.exceptions.clients.InvalidRunningTaskException
import io.infinitic.exceptions.clients.UnknownException
import io.infinitic.exceptions.clients.UnknownTaskException
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture

internal class ClientDispatcherImpl(
    val scope: CoroutineScope,
    val clientName: ClientName,
    val sendToTaskEngine: SendToTaskEngine,
    val sendToWorkflowEngine: SendToWorkflowEngine,
    val sendToTaskTagEngine: SendToTaskTagEngine,
    val sendToWorkflowTagEngine: SendToWorkflowTagEngine
) : ClientDispatcher {
    val logger = KotlinLogging.logger {}

    private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

    companion object {
        @JvmStatic
        private val localLastDeferred: ThreadLocal<Deferred<*>?> = ThreadLocal()
    }

    override suspend fun handle(message: ClientMessage) {
        responseFlow.emit(message)
    }

    override fun getLastDeferred(): Deferred<*>? = localLastDeferred.get()

    // asynchronous call: dispatch(stub::method)(*args)
    override fun <R : Any?> dispatchAsync(
        handler: ProxyHandler<*>
    ): CompletableFuture<Deferred<R>> = when (handler) {
        is NewTaskProxyHandler -> dispatchTaskAsync(handler)
        is NewWorkflowProxyHandler -> dispatchWorkflowAsync(handler)
        is GetTaskProxyHandler -> throw InvalidRunningTaskException("${handler.stub()}")
        is GetWorkflowProxyHandler -> dispatchMethodAsync(handler)
        is ChannelProxyHandler -> dispatchSignalAsync(handler)
    }

    override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R = when (handler) {
        is NewTaskProxyHandler -> dispatchTaskAndWait(handler)
        is NewWorkflowProxyHandler -> dispatchWorkflowAndWait(handler)
        is GetTaskProxyHandler -> throw InvalidRunningTaskException("${handler.stub()}")
        is GetWorkflowProxyHandler -> dispatchMethodAndWait(handler)
        is ChannelProxyHandler -> dispatchSignalAndWait(handler)
    }

    override fun <T> awaitTask(
        returnClass: Class<T>,
        taskName: TaskName,
        taskId: TaskId,
        clientWaiting: Boolean
    ): T {
        // if task was initially not sync, then send WaitTask message
        if (! clientWaiting) {
            val waitTask = WaitTask(
                taskName = taskName,
                taskId = taskId,
                emitterName = clientName
            )
            scope.launch { sendToTaskEngine(waitTask) }
        }

        // wait for result
        val taskResult = scope.future {
            responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                it is TaskMessage && it.taskId == taskId
            }
        }.join()

        @Suppress("UNCHECKED_CAST")
        return when (taskResult) {
            is TaskCompleted -> taskResult.taskReturnValue.value() as T
            is TaskCanceled -> throw CanceledException(
                "$taskId",
                "$taskName",
            )
            is TaskFailed -> throw FailedException(
                "$taskId",
                "$taskName",
                taskResult.error
            )
            is TaskUnknown -> throw UnknownTaskException(
                "$taskId",
                "$taskName"
            )
            else -> thisShouldNotHappen("Unexpected ${taskResult::class}")
        }
    }

    override fun <T> awaitWorkflow(
        returnClass: Class<T>,
        workflowName: WorkflowName,
        workflowId: WorkflowId,
        methodRunId: MethodRunId?,
        clientWaiting: Boolean
    ): T {
        // if task was not initially sync, then send WaitTask message
        if (! clientWaiting) {
            val waitWorkflow = WaitWorkflow(
                workflowName = workflowName,
                workflowId = workflowId,
                methodRunId = methodRunId,
                emitterName = clientName
            )
            scope.launch {
                sendToWorkflowEngine(waitWorkflow)
            }
        }

        // wait for result
        val workflowResult = scope.future {
            responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                (it is MethodMessage && it.workflowId == workflowId && it.methodRunId == methodRunId)
            }
        }.join()

        @Suppress("UNCHECKED_CAST")
        return when (workflowResult) {
            is CompletedMethod -> workflowResult.methodReturnValue.value() as T
            is CanceledMethod -> throw CanceledException(
                "$workflowId",
                "$workflowName"
            )
            is FailedMethod -> throw FailedException(
                "$workflowId",
                "$workflowName",
                workflowResult.error
            )
            is UnknownMethod -> throw UnknownException(
                "$workflowId",
                "$workflowName"
            )
            else -> thisShouldNotHappen("Unexpected ${workflowResult::class}")
        }
    }

    override fun completeTaskAsync(
        taskName: TaskName,
        taskId: TaskId?,
        taskTag: TaskTag?,
        value: Any?
    ): CompletableFuture<Unit> = TODO("Not yet implemented")

    override fun completeWorkflowAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?,
        value: Any?
    ): CompletableFuture<Unit> = TODO("Not yet implemented")

    override fun cancelTaskAsync(
        taskName: TaskName,
        taskId: TaskId?,
        taskTag: TaskTag?
    ): CompletableFuture<Unit> = scope.future {
        when {
            taskId != null -> {
                val msg = CancelTask(
                    taskName = taskName,
                    taskId = taskId,
                    emitterName = clientName
                )
                sendToTaskEngine(msg)
            }
            taskTag != null -> {
                val msg = CancelTaskByTag(
                    taskName = taskName,
                    taskTag = taskTag,
                    emitterName = clientName
                )
                sendToTaskTagEngine(msg)
            }
            else -> thisShouldNotHappen()
        }
    }

    override fun cancelWorkflowAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        methodRunId: MethodRunId?,
        workflowTag: WorkflowTag?
    ): CompletableFuture<Unit> = scope.future {
        when {
            workflowId != null -> {
                val msg = CancelWorkflow(
                    workflowName = workflowName,
                    workflowId = workflowId,
                    methodRunId = methodRunId,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                    emitterName = clientName
                )
                sendToWorkflowEngine(msg)
            }
            workflowTag != null -> {
                val msg = CancelWorkflowByTag(
                    workflowName = workflowName,
                    workflowTag = workflowTag,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT,
                    emitterWorkflowId = null,
                    emitterName = clientName
                )
                sendToWorkflowTagEngine(msg)
            }
            else -> thisShouldNotHappen()
        }
    }

    override fun retryTaskAsync(
        taskName: TaskName,
        taskId: TaskId?,
        taskTag: TaskTag?
    ): CompletableFuture<Unit> = scope.future {
        when {
            taskId != null -> {
                val msg = RetryTask(
                    taskName = taskName,
                    taskId = taskId,
                    emitterName = clientName
                )
                sendToTaskEngine(msg)
            }
            taskTag != null -> {
                val msg = RetryTaskByTag(
                    taskName = taskName,
                    taskTag = taskTag,
                    emitterName = clientName
                )
                sendToTaskTagEngine(msg)
            }
            else -> thisShouldNotHappen()
        }
    }

    override fun retryWorkflowAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): CompletableFuture<Unit> = scope.future {
        when {
            workflowId != null -> {
                val msg = RetryWorkflowTask(
                    workflowName = workflowName,
                    workflowId = workflowId,
                    emitterName = clientName
                )
                sendToWorkflowEngine(msg)
            }
            workflowTag != null -> {
                val msg = RetryWorkflowTaskByTag(
                    workflowName = workflowName,
                    workflowTag = workflowTag,
                    emitterWorkflowId = null,
                    emitterName = clientName
                )
                sendToWorkflowTagEngine(msg)
            }
            else -> thisShouldNotHappen()
        }
    }

    // synchronously get task ids per tag
    override fun getTaskIdsByTag(
        taskName: TaskName,
        taskId: TaskId?,
        taskTag: TaskTag?
    ): Set<String> = when {
        taskId != null ->
            setOf(taskId.toString())
        taskTag != null -> {
            val taskIdsPerTag = scope.future {
                val msg = GetTaskIdsByTag(
                    taskName = taskName,
                    taskTag = taskTag,
                    emitterName = clientName
                )
                launch { sendToTaskTagEngine(msg) }

                responseFlow.first {
                    it is TaskIdsPerTag && it.taskName == taskName && it.taskTag == taskTag
                } as TaskIdsPerTag
            }.join()

            taskIdsPerTag.taskIds.map { it.toString() }.toSet()
        }
        else ->
            thisShouldNotHappen()
    }

    override fun getWorkflowIdsByTag(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): Set<String> = when {
        workflowId != null ->
            setOf(workflowId.toString())
        workflowTag != null -> {
            val workflowIdsPerTag = scope.future {
                val msg = GetWorkflowIdsByTag(
                    workflowName = workflowName,
                    workflowTag = workflowTag,
                    emitterName = clientName
                )
                launch { sendToWorkflowTagEngine(msg) }

                responseFlow.first {
                    logger.debug { "ResponseFlow: $it" }
                    it is WorkflowIdsPerTag && it.workflowName == workflowName && it.workflowTag == workflowTag
                } as WorkflowIdsPerTag
            }.join()

            workflowIdsPerTag.workflowIds.map { it.toString() }.toSet()
        }
        else ->
            thisShouldNotHappen()
    }

    // asynchronous call: dispatch(stub::method)(*args)
    @Suppress("UNCHECKED_CAST")
    private fun <R : Any?> dispatchTaskAsync(
        handler: NewTaskProxyHandler<*>
    ): CompletableFuture<Deferred<R>> {
        val deferredTask: DeferredTask<R> =
            deferredTask(handler.method.returnType as Class<R>, handler.taskName, clientWaiting = false)

        return scope.future { dispatchTask(deferredTask, handler); deferredTask }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <R> dispatchTaskAndWait(
        handler: NewTaskProxyHandler<*>
    ): R {
        val deferredTask: DeferredTask<R> =
            deferredTask(handler.method.returnType as Class<R>, handler.taskName, clientWaiting = true)

        scope.launch { dispatchTask(deferredTask, handler) }

        return deferredTask.await()
    }

    private fun <R> deferredTask(
        returnClass: Class<R>,
        taskName: TaskName,
        clientWaiting: Boolean,
    ): DeferredTask<R> {
        val deferredTask = DeferredTask(returnClass, taskName, TaskId(), clientWaiting, this)

        // store in ThreadLocal to be used in ::getLastDeferred
        localLastDeferred.set(deferredTask)

        return deferredTask
    }

    private fun <R : Any?> dispatchTask(
        deferred: DeferredTask<R>,
        handler: NewTaskProxyHandler<*>
    ) {
        scope.future {
            coroutineScope {
                // add provided tags for this id
                handler.taskTags.map {
                    val addTagToTask = AddTagToTask(
                        taskName = deferred.taskName,
                        taskTag = it,
                        taskId = deferred.taskId,
                        emitterName = clientName
                    )
                    launch { sendToTaskTagEngine(addTagToTask) }
                }

                // dispatch this task
                val dispatchTask = DispatchTask(
                    taskName = deferred.taskName,
                    taskId = deferred.taskId,
                    taskOptions = handler.taskOptions,
                    clientWaiting = deferred.clientWaiting,
                    methodName = handler.methodName,
                    methodParameterTypes = handler.methodParameterTypes,
                    methodParameters = handler.methodParameters,
                    workflowId = null,
                    workflowName = null,
                    methodRunId = null,
                    taskTags = handler.taskTags,
                    taskMeta = handler.taskMeta,
                    emitterName = clientName
                )
                launch { sendToTaskEngine(dispatchTask) }
            }
        }.join()
    }

    // asynchronous call: dispatch(stub::method)(*args)
    @Suppress("UNCHECKED_CAST")
    private fun <R : Any?> dispatchWorkflowAsync(
        handler: NewWorkflowProxyHandler<*>,
    ): CompletableFuture<Deferred<R>> = when (handler.isChannelGetter()) {
        true ->
            throw InvalidChannelUsageException()
        false -> {
            val deferredWorkflow = deferredWorkflow(handler.method.returnType as Class<R>, handler.workflowName, clientWaiting = false)

            scope.future { dispatchWorkflow(deferredWorkflow, handler); deferredWorkflow }
        }
    }

    // asynchronous call: dispatch(stub::method)(*args)
    @Suppress("UNCHECKED_CAST")
    private fun <R : Any?> dispatchWorkflowAndWait(
        handler: NewWorkflowProxyHandler<*>,
    ): R = when (handler.isChannelGetter()) {
        true ->
            throw InvalidChannelUsageException()
        false -> {
            val deferredWorkflow = deferredWorkflow<R>(handler.method.returnType as Class<R>, handler.workflowName, clientWaiting = true)

            scope.launch { dispatchWorkflow(deferredWorkflow, handler) }

            deferredWorkflow.await()
        }
    }

    private fun <R : Any?> deferredWorkflow(
        returnClass: Class<R>,
        workflowName: WorkflowName,
        clientWaiting: Boolean,
    ): DeferredWorkflow<R> {
        val workflowId = WorkflowId()

        val deferredWorkflow = DeferredWorkflow<R>(returnClass, workflowName, workflowId, clientWaiting, this)

        // store in ThreadLocal to be used in ::getDeferred
        localLastDeferred.set(deferredWorkflow)

        return deferredWorkflow
    }

    private fun <R : Any?> dispatchWorkflow(
        deferred: DeferredWorkflow<R>,
        handler: NewWorkflowProxyHandler<*>
    ) {
        scope.future {
            coroutineScope {
                // add provided tags
                handler.workflowTags.map {
                    val addTagToWorkflow = AddTagToWorkflow(
                        workflowName = deferred.workflowName,
                        workflowTag = it,
                        workflowId = deferred.workflowId,
                        emitterName = clientName
                    )
                    launch { sendToWorkflowTagEngine(addTagToWorkflow) }
                }
                // dispatch workflow
                val dispatchWorkflow = DispatchWorkflow(
                    workflowName = deferred.workflowName,
                    workflowId = deferred.workflowId,
                    methodName = handler.methodName,
                    methodParameters = handler.methodParameters,
                    methodParameterTypes = handler.methodParameterTypes,
                    workflowOptions = handler.workflowOptions,
                    workflowTags = handler.workflowTags,
                    workflowMeta = handler.workflowMeta,
                    parentWorkflowName = null,
                    parentWorkflowId = null,
                    parentMethodRunId = null,
                    clientWaiting = deferred.clientWaiting,
                    emitterName = clientName
                )
                launch { sendToWorkflowEngine(dispatchWorkflow) }
            }
        }.join()
    }

    // asynchronous call: dispatch(stub::method)(*args)
    @Suppress("UNCHECKED_CAST")
    private fun <R : Any?> dispatchMethodAsync(
        handler: GetWorkflowProxyHandler<*>
    ): CompletableFuture<Deferred<R>> = when (handler.isChannelGetter()) {
        true -> {
            // special case of getting a channel from a workflow
            @Suppress("UNCHECKED_CAST")
            val channel = ChannelProxyHandler<SendChannel<*>>(handler).stub()
            @Suppress("UNCHECKED_CAST")
            CompletableFuture.completedFuture(DeferredChannel(channel) as Deferred<R>)
        }
        false -> {
            val deferredMethod = deferredMethod(
                handler.method.returnType as Class<R>,
                handler.workflowName,
                handler.workflowId,
                handler.workflowTag,
                clientWaiting = false
            )

            scope.future { dispatchMethod(deferredMethod, handler); deferredMethod }
        }
    }

    // asynchronous call: dispatch(stub::method)(*args)
    @Suppress("UNCHECKED_CAST")
    private fun <R : Any?> dispatchMethodAndWait(
        handler: GetWorkflowProxyHandler<*>
    ): R = when (handler.isChannelGetter()) {
        true -> {
            // special case of getting a channel from a workflow
            @Suppress("UNCHECKED_CAST")
            ChannelProxyHandler<SendChannel<*>>(handler).stub() as R
        }
        false -> {
            val deferredMethod = deferredMethod(
                handler.method.returnType as Class<R>,
                handler.workflowName,
                handler.workflowId,
                handler.workflowTag,
                clientWaiting = true
            )

            scope.launch { dispatchMethod(deferredMethod, handler) }

            deferredMethod.await()
        }
    }

    private fun <R : Any?> deferredMethod(
        returnClass: Class<R>,
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?,
        clientWaiting: Boolean,
    ): DeferredMethod<R> {
        val methodRunId = MethodRunId()
        val deferredMethod = when {
            workflowId != null ->
                DeferredMethod(returnClass, workflowName, workflowId, methodRunId, null, clientWaiting, this)
            workflowTag != null ->
                DeferredMethod(returnClass, workflowName, null, methodRunId, workflowTag, clientWaiting, this)
            else ->
                thisShouldNotHappen()
        }

        // store in ThreadLocal to be used in ::getDeferred
        localLastDeferred.set(deferredMethod)

        return deferredMethod
    }

    private fun <R : Any?> dispatchMethod(
        deferred: DeferredMethod<R>,
        handler: GetWorkflowProxyHandler<*>
    ) = when {
        deferred.workflowId != null && deferred.methodRunId != null -> {
            val dispatchMethod = DispatchMethod(
                workflowName = deferred.workflowName,
                workflowId = deferred.workflowId,
                methodRunId = deferred.methodRunId,
                methodName = handler.methodName,
                methodParameters = handler.methodParameters,
                methodParameterTypes = handler.methodParameterTypes,
                parentWorkflowId = null,
                parentWorkflowName = null,
                parentMethodRunId = null,
                clientWaiting = deferred.clientWaiting,
                emitterName = clientName
            )
            sendToWorkflowEngine(dispatchMethod)
        }
        deferred.workflowTag != null -> {
            val dispatchMethodByTag = DispatchMethodByTag(
                workflowName = deferred.workflowName,
                workflowTag = deferred.workflowTag,
                parentWorkflowId = null,
                parentWorkflowName = null,
                parentMethodRunId = null,
                methodRunId = MethodRunId(),
                methodName = handler.methodName,
                methodParameterTypes = handler.methodParameterTypes,
                methodParameters = handler.methodParameters,
                clientWaiting = deferred.clientWaiting,
                emitterName = clientName
            )
            sendToWorkflowTagEngine(dispatchMethodByTag)
        }
        else ->
            thisShouldNotHappen()
    }

    // asynchronous call: dispatch(stub.channel::send, signal)
    private fun <S : Any?> dispatchSignalAsync(
        handler: ChannelProxyHandler<*>
    ): CompletableFuture<Deferred<S>> {

        val deferredSend = deferredSend<S>()

        return scope.future { dispatchSignal(deferredSend, handler); deferredSend }
    }

    // synchronous call: stub.channel.send(signal)
    private fun <S : Any?> dispatchSignalAndWait(
        handler: ChannelProxyHandler<*>
    ): S {

        val deferredSend = deferredSend<S>()

        // synchronous call
        dispatchSignal(deferredSend, handler)

        return deferredSend.await()
    }

    // asynchronous call: dispatch(stub.channel::send, signal)
    private fun dispatchSignal(
        deferredSend: DeferredSend<*>,
        handler: ChannelProxyHandler<*>
    ) {
        if (handler.methodName.toString() != SendChannel<*>::send.name) thisShouldNotHappen()

        when {
            handler.workflowId != null -> {
                val sendSignal = SendSignal(
                    workflowName = handler.workflowName,
                    workflowId = handler.workflowId!!,
                    channelName = handler.channelName,
                    channelSignalId = deferredSend.channelSignalId,
                    channelSignal = handler.channelSignal,
                    channelSignalTypes = handler.channelSignalTypes,
                    emitterName = clientName
                )
                sendToWorkflowEngine(sendSignal)
            }
            handler.workflowTag != null -> {
                val sendSignalByTag = SendSignalByTag(
                    workflowName = handler.workflowName,
                    workflowTag = handler.workflowTag!!,
                    channelName = handler.channelName,
                    channelSignalId = deferredSend.channelSignalId,
                    channelSignal = handler.channelSignal,
                    channelSignalTypes = handler.channelSignalTypes,
                    emitterWorkflowId = null,
                    emitterName = clientName
                )
                sendToWorkflowTagEngine(sendSignalByTag)
            }
            else ->
                thisShouldNotHappen()
        }
    }

    private fun <R : Any?> deferredSend(): DeferredSend<R> {
        val deferredSend = DeferredSend<R>(ChannelSignalId())

        // store in ThreadLocal to be used in ::getLastDeferred
        localLastDeferred.set(deferredSend)

        return deferredSend
    }
}
