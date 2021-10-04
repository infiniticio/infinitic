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
import io.infinitic.client.deferred.DeferredSend
import io.infinitic.client.deferred.DeferredTask
import io.infinitic.client.deferred.DeferredWorkflow
import io.infinitic.common.clients.data.ClientName
import io.infinitic.common.clients.messages.ClientMessage
import io.infinitic.common.clients.messages.MethodAlreadyCompleted
import io.infinitic.common.clients.messages.MethodCanceled
import io.infinitic.common.clients.messages.MethodCompleted
import io.infinitic.common.clients.messages.MethodFailed
import io.infinitic.common.clients.messages.MethodUnknown
import io.infinitic.common.clients.messages.TaskCanceled
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskFailed
import io.infinitic.common.clients.messages.TaskIdsPerTag
import io.infinitic.common.clients.messages.TaskUnknown
import io.infinitic.common.clients.messages.WorkflowIdsPerTag
import io.infinitic.common.clients.messages.interfaces.MethodMessage
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.GetTaskProxyHandler
import io.infinitic.common.proxies.GetWorkflowProxyHandler
import io.infinitic.common.proxies.Method
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.AddTaskTag
import io.infinitic.common.tasks.tags.messages.CancelTaskPerTag
import io.infinitic.common.tasks.tags.messages.GetTaskIds
import io.infinitic.common.tasks.tags.messages.RetryTaskPerTag
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.methodRuns.MethodRunId
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchMethodRun
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskPerTag
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.exceptions.clients.AlreadyCompletedException
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
    val sendToTaskTagEngine: SendToTaskTagEngine,
    val sendToTaskEngine: SendToTaskEngine,
    val sendToWorkflowTagEngine: SendToWorkflowTagEngine,
    val sendToWorkflowEngine: SendToWorkflowEngine
) : ClientDispatcher {
    private val logger = KotlinLogging.logger {}

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
        is ChannelProxyHandler -> dispatchSendAsync(handler)
    }

    override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R = when (handler) {
        is NewTaskProxyHandler -> dispatchTaskAndWait(handler)
        is NewWorkflowProxyHandler -> dispatchWorkflowAndWait(handler)
        is GetTaskProxyHandler -> throw InvalidRunningTaskException("${handler.stub()}")
        is GetWorkflowProxyHandler -> dispatchMethodAndWait(handler)
        is ChannelProxyHandler -> dispatchSendAndWait(handler)
    }

    override fun <R : Any?> awaitTask(taskName: TaskName, taskId: TaskId, clientWaiting: Boolean): R {
        // if task was initially not sync, then send WaitTask message
        if (! clientWaiting) {
            val waitTask = WaitTask(
                taskId = taskId,
                taskName = taskName,
                clientName = clientName
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
            is TaskCompleted -> taskResult.taskReturnValue.get() as R
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

    override fun <R : Any?> awaitWorkflow(
        workflowName: WorkflowName,
        workflowId: WorkflowId,
        methodRunId: MethodRunId,
        clientWaiting: Boolean
    ): R {
        // if task was not initially sync, then send WaitTask message
        if (! clientWaiting) {
            val waitWorkflow = WaitWorkflow(
                workflowName = workflowName,
                workflowId = workflowId,
                methodRunId = methodRunId,
                clientName = clientName
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
            is MethodCompleted -> workflowResult.workflowReturnValue.get() as R
            is MethodCanceled -> throw CanceledException(
                "$workflowId",
                "$workflowName"
            )
            is MethodFailed -> throw FailedException(
                "$workflowId",
                "$workflowName",
                workflowResult.error
            )
            is MethodUnknown -> throw UnknownException(
                "$workflowId",
                "$workflowName"
            )
            is MethodAlreadyCompleted -> throw AlreadyCompletedException(
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
                    taskId = taskId,
                    taskName = taskName
                )
                sendToTaskEngine(msg)
            }
            taskTag != null -> {
                val msg = CancelTaskPerTag(
                    taskTag = taskTag,
                    taskName = taskName
                )
                sendToTaskTagEngine(msg)
            }
            else -> throw thisShouldNotHappen()
        }
    }

    override fun cancelWorkflowAsync(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): CompletableFuture<Unit> = scope.future {
        when {
            workflowId != null -> {
                val msg = CancelWorkflow(
                    workflowId = workflowId,
                    workflowName = workflowName,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                )
                sendToWorkflowEngine(msg)
            }
            workflowTag != null -> {
                val msg = CancelWorkflowPerTag(
                    workflowTag = workflowTag,
                    workflowName = workflowName,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
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
                    taskId = taskId,
                    taskName = taskName
                )
                sendToTaskEngine(msg)
            }
            taskTag != null -> {
                val msg = RetryTaskPerTag(
                    taskTag = taskTag,
                    taskName = taskName
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
                    workflowId = workflowId,
                    workflowName = workflowName
                )
                sendToWorkflowEngine(msg)
            }
            workflowTag != null -> {
                val msg = RetryWorkflowTaskPerTag(
                    workflowTag = workflowTag,
                    workflowName = workflowName
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
        taskId != null -> setOf(taskId.toString())
        taskTag != null -> {
            val taskIdsPerTag = scope.future() {
                val msg = GetTaskIds(
                    taskTag = taskTag,
                    taskName = taskName,
                    clientName = clientName
                )
                launch { sendToTaskTagEngine(msg) }

                responseFlow.first {
                    it is TaskIdsPerTag && it.taskName == taskName && it.taskTag == taskTag
                } as TaskIdsPerTag
            }.join()

            taskIdsPerTag.taskIds.map { it.toString() }.toSet()
        }
        else -> throw thisShouldNotHappen()
    }

    override fun getWorkflowIdsByTag(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?
    ): Set<String> = when {
        workflowId != null -> setOf(workflowId.toString())
        workflowTag != null -> {
            val workflowIdsPerTag = scope.future() {
                val msg = GetWorkflowIds(
                    clientName = clientName,
                    workflowTag = workflowTag,
                    workflowName = workflowName
                )
                launch { sendToWorkflowTagEngine(msg) }

                responseFlow.first {
                    logger.debug { "ResponseFlow: $it" }
                    it is WorkflowIdsPerTag && it.workflowName == workflowName && it.workflowTag == workflowTag
                } as WorkflowIdsPerTag
            }.join()

            workflowIdsPerTag.workflowIds.map { it.toString() }.toSet()
        }
        else -> thisShouldNotHappen()
    }

    // asynchronous call: dispatch(stub::method)(*args)
    private fun <R : Any?> dispatchTaskAsync(
        handler: NewTaskProxyHandler<*>
    ): CompletableFuture<Deferred<R>> {
        val deferredTask = deferredTask<R>(handler.taskName, clientWaiting = false)

        return scope.future {
            dispatchTask(
                deferredTask,
                handler.method(),
                handler.taskTags,
                handler.taskOptions,
                handler.taskMeta
            )

            deferredTask
        }
    }

    private fun <R : Any?> dispatchTaskAndWait(
        handler: NewTaskProxyHandler<*>
    ): R {
        val deferredTask = deferredTask<R>(handler.taskName, clientWaiting = true)

        scope.launch {
            dispatchTask(
                deferredTask,
                handler.method(),
                handler.taskTags,
                handler.taskOptions,
                handler.taskMeta
            )
        }

        return deferredTask.await()
    }

    private fun <R : Any?> deferredTask(
        taskName: TaskName,
        clientWaiting: Boolean,
    ): DeferredTask<R> {
        val deferredTask = DeferredTask<R>(taskName, TaskId(), clientWaiting, this)

        // store in ThreadLocal to be used in ::getLastDeferred
        localLastDeferred.set(deferredTask)

        return deferredTask
    }

    private fun <R : Any?> dispatchTask(
        deferred: DeferredTask<R>,
        method: Method,
        tags: Set<TaskTag>,
        options: TaskOptions,
        meta: TaskMeta
    ) {
        scope.future {
            coroutineScope {
                // add provided tags for this id
                tags.map {
                    val addTaskTag = AddTaskTag(
                        taskTag = it,
                        taskName = deferred.taskName,
                        taskId = deferred.taskId
                    )
                    launch { sendToTaskTagEngine(addTaskTag) }
                }

                // dispatch this task
                val dispatchTask = DispatchTask(
                    taskId = deferred.taskId,
                    taskName = deferred.taskName,
                    clientName = clientName,
                    clientWaiting = deferred.clientWaiting,
                    methodName = method.methodName,
                    methodParameterTypes = method.methodParameterTypes,
                    methodParameters = method.methodParameters,
                    workflowId = null,
                    workflowName = null,
                    methodRunId = null,
                    taskTags = tags,
                    taskOptions = options,
                    taskMeta = meta
                )
                launch { sendToTaskEngine(dispatchTask) }
            }
        }.join()
    }

    // asynchronous call: dispatch(stub::method)(*args)
    private fun <R : Any?> dispatchWorkflowAsync(
        handler: NewWorkflowProxyHandler<*>,
    ): CompletableFuture<Deferred<R>> = when (handler.isChannelGetter()) {
        true ->
            // special case of getting a channel from a workflow
            throw InvalidChannelUsageException()
        false -> {
            val deferredWorkflow = deferredWorkflow<R>(handler.workflowName, clientWaiting = false)

            scope.future {
                dispatchWorkflow(
                    deferredWorkflow,
                    handler.method(),
                    handler.workflowTags,
                    handler.workflowOptions,
                    handler.workflowMeta
                )

                deferredWorkflow
            }
        }
    }

    // asynchronous call: dispatch(stub::method)(*args)
    private fun <R : Any?> dispatchWorkflowAndWait(
        handler: NewWorkflowProxyHandler<*>,
    ): R = when (handler.isChannelGetter()) {
        true ->
            // special case of getting a channel from a workflow
            throw InvalidChannelUsageException()
        false -> {
            val deferredWorkflow = deferredWorkflow<R>(handler.workflowName, clientWaiting = true)

            scope.launch {
                dispatchWorkflow(
                    deferredWorkflow,
                    handler.method(),
                    handler.workflowTags,
                    handler.workflowOptions,
                    handler.workflowMeta
                )
            }

            deferredWorkflow.await()
        }
    }

    private fun <R : Any?> deferredWorkflow(
        workflowName: WorkflowName,
        clientWaiting: Boolean,
    ): DeferredWorkflow<R> {
        val workflowId = WorkflowId()
        val methodRunId = MethodRunId.from(workflowId)

        val deferredWorkflow = DeferredWorkflow<R>(workflowName, workflowId, methodRunId, clientWaiting, this)

        // store in ThreadLocal to be used in ::getDeferred
        localLastDeferred.set(deferredWorkflow)

        return deferredWorkflow
    }

    private fun <R : Any?> dispatchWorkflow(
        deferred: DeferredWorkflow<R>,
        method: Method,
        tags: Set<WorkflowTag>,
        options: WorkflowOptions,
        meta: WorkflowMeta
    ) {
        scope.future {
            coroutineScope {
                // add provided tags
                tags.map {
                    val addWorkflowTag = AddWorkflowTag(
                        workflowTag = it,
                        workflowName = deferred.workflowName,
                        workflowId = deferred.workflowId
                    )
                    launch { sendToWorkflowTagEngine(addWorkflowTag) }
                }
                // dispatch workflow
                val dispatchWorkflow = DispatchWorkflow(
                    workflowId = deferred.workflowId,
                    workflowName = deferred.workflowName,
                    clientName = clientName,
                    clientWaiting = deferred.clientWaiting,
                    parentWorkflowId = null,
                    parentWorkflowName = null,
                    parentMethodRunId = null,
                    methodName = method.methodName,
                    methodParameterTypes = method.methodParameterTypes,
                    methodParameters = method.methodParameters,
                    workflowTags = tags,
                    workflowOptions = options,
                    workflowMeta = meta
                )
                launch { sendToWorkflowEngine(dispatchWorkflow) }
            }
        }.join()
    }

    // asynchronous call: dispatch(stub::method)(*args)
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
            val deferredMethod = deferredMethod<R>(handler.workflowName, handler.workflowId, handler.workflowTag, clientWaiting = false)

            scope.future {
                dispatchMethod(deferredMethod, handler.method())

                deferredMethod
            }
        }
    }

    // asynchronous call: dispatch(stub::method)(*args)
    private fun <R : Any?> dispatchMethodAndWait(
        handler: GetWorkflowProxyHandler<*>
    ): R = when (handler.isChannelGetter()) {
        true -> {
            // special case of getting a channel from a workflow
            @Suppress("UNCHECKED_CAST")
            ChannelProxyHandler<SendChannel<*>>(handler).stub() as R
        }
        false -> {
            val deferredMethod = deferredMethod<R>(handler.workflowName, handler.workflowId, handler.workflowTag, clientWaiting = true)

            scope.launch {
                dispatchMethod(deferredMethod, handler.method())
            }

            deferredMethod.await()
        }
    }

    private fun <R : Any?> deferredMethod(
        workflowName: WorkflowName,
        workflowId: WorkflowId?,
        workflowTag: WorkflowTag?,
        clientWaiting: Boolean,
    ): DeferredWorkflow<R> = when {
        workflowId != null -> {
            val deferredMethod = DeferredWorkflow<R>(workflowName, workflowId, MethodRunId.random(), clientWaiting, this)

            // store in ThreadLocal to be used in ::getDeferred
            localLastDeferred.set(deferredMethod)

            deferredMethod
        }
        workflowTag != null -> TODO("Not yet implemented")
        else -> throw thisShouldNotHappen()
    }

    private fun <R : Any?> dispatchMethod(
        deferred: DeferredWorkflow<R>,
        method: Method
    ) {
        val dispatchMethodRun = DispatchMethodRun(
            workflowId = deferred.workflowId,
            workflowName = deferred.workflowName,
            methodRunId = deferred.methodRunId,
            clientName = clientName,
            clientWaiting = deferred.clientWaiting,
            parentWorkflowId = null,
            parentWorkflowName = null,
            parentMethodRunId = null,
            methodName = method.methodName,
            methodParameterTypes = method.methodParameterTypes,
            methodParameters = method.methodParameters
        )
        sendToWorkflowEngine(dispatchMethodRun)
    }

    // asynchronous call: dispatch(stub.channel::send, signal)
    private fun <S : Any?> dispatchSendAsync(
        handler: ChannelProxyHandler<*>
    ): CompletableFuture<Deferred<S>> {

        val deferredSend = deferredSend<S>()

        return scope.future {
            dispatchSend(deferredSend, handler)

            deferredSend
        }
    }

    // synchronous call: stub.channel.send(signal)
    private fun <S : Any?> dispatchSendAndWait(
        handler: ChannelProxyHandler<*>
    ): S {

        val deferredSend = deferredSend<S>()

        // synchronous call
        dispatchSend(deferredSend, handler)

        return deferredSend.await()
    }

    // asynchronous call: dispatch(stub.channel::send, signal)
    private fun dispatchSend(
        deferredSend: DeferredSend<*>,
        handler: ChannelProxyHandler<*>
    ) {
        if (handler.methodName != SendChannel<*>::send.name) throw thisShouldNotHappen()

        when {
            handler.workflowId != null -> {
                val sendToChannel = SendToChannel(
                    workflowId = handler.workflowId!!,
                    workflowName = handler.workflowName,
                    clientName = clientName,
                    channelSignalId = deferredSend.channelSignalId,
                    channelName = handler.channelName,
                    channelSignal = handler.channelSignal,
                    channelSignalTypes = handler.channelSignalTypes
                )
                sendToWorkflowEngine(sendToChannel)
            }
            handler.workflowTag != null -> {
                val sendToChannelPerTag = SendToChannelPerTag(
                    workflowTag = handler.workflowTag!!,
                    workflowName = handler.workflowName,
                    clientName = clientName,
                    channelSignalId = deferredSend.channelSignalId,
                    channelName = handler.channelName,
                    channelSignal = handler.channelSignal,
                    channelSignalTypes = handler.channelSignalTypes
                )
                sendToWorkflowTagEngine(sendToChannelPerTag)
            }
            else ->
                throw thisShouldNotHappen()
        }
    }

    private fun <R : Any?> deferredSend(): DeferredSend<R> {
        val deferredSend = DeferredSend<R>(ChannelSignalId())

        // store in ThreadLocal to be used in ::getLastDeferred
        localLastDeferred.set(deferredSend)

        return deferredSend
    }
}
