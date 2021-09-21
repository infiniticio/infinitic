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
import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ChannelProxyHandler
import io.infinitic.common.proxies.Method
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
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
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.channels.ChannelSignal
import io.infinitic.common.workflows.data.channels.ChannelSignalId
import io.infinitic.common.workflows.data.channels.ChannelSignalType
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
import io.infinitic.exceptions.clients.InvalidWorkflowException
import io.infinitic.exceptions.clients.UnknownException
import io.infinitic.exceptions.clients.UnknownTaskException
import io.infinitic.exceptions.thisShouldNotHappen
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.UUID
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

    override suspend fun handle(message: ClientMessage) {
        responseFlow.emit(message)
    }

    // asynchronous call: dispatch(stub::method)(*args)
    override fun <R : Any?> dispatch(
        handler: ProxyHandler<*>,
        clientWaiting: Boolean,
        tags: Set<String>?,
        options: JobOptions?,
        meta: Map<String, ByteArray>?,
    ): Deferred<R> = when (handler) {
        is TaskProxyHandler ->
            dispatchNewTask(
                handler.taskName,
                handler.method(),
                clientWaiting,
                tags?.map { TaskTag(it) }?.toSet() ?: handler.taskTags,
                (options as TaskOptions?) ?: handler.taskOptions,
                meta?.run { TaskMeta(this) } ?: handler.taskMeta
            )
        is WorkflowProxyHandler -> when (handler.isMethodChannel()) {
            // special case of getting a channel from a workflow
            true -> {
                @Suppress("UNCHECKED_CAST")
                val channel = ChannelProxyHandler<SendChannel<*>>(handler).stub()
                @Suppress("UNCHECKED_CAST")
                DeferredChannel(channel) as Deferred<R>
            }
            false -> dispatchNewWorkflow(
                handler.workflowName,
                handler.method(),
                clientWaiting,
                tags?.map { WorkflowTag(it) }?.toSet() ?: handler.workflowTags,
                (options as WorkflowOptions?) ?: handler.workflowOptions,
                meta?.run { WorkflowMeta(this) } ?: handler.workflowMeta
            )
        }
        is ChannelProxyHandler ->
            throw InvalidChannelUsageException()
    }

    // asynchronous call: dispatch(stub::method, id)(*args)
    override fun <R> dispatch(handler: ProxyHandler<*>, id: UUID): Deferred<R> =
        when (handler is WorkflowProxyHandler && ! handler.isMethodChannel()) {
            true -> dispatchMethod(
                handler.workflowName,
                WorkflowId(id),
                handler.method()
            )
            false -> throw InvalidWorkflowException("${handler.stub()}")
        }

    // asynchronous call: dispatch(stub::method, tag)(*args)
    override fun <R> dispatch(handler: ProxyHandler<*>, tag: String): Deferred<R> =
        when (handler is WorkflowProxyHandler && ! handler.isMethodChannel()) {
            true -> dispatchMethod(
                handler.workflowName,
                WorkflowTag(tag),
                handler.method()
            )
            false -> throw InvalidWorkflowException("${handler.stub()}")
        }

    override fun <R : Any?> awaitTask(taskName: TaskName, taskId: TaskId, clientWaiting: Boolean): R {
        val taskResult = scope.future {
            // if task was initially not sync, then send WaitTask message
            if (! clientWaiting) {
                val waitTask = WaitTask(
                    taskId = taskId,
                    taskName = taskName,
                    clientName = clientName
                )
                launch { sendToTaskEngine(waitTask) }
            }
            // wait for result
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
        val workflowResult = scope.future {
            // if task was not initially sync, then send WaitTask message
            if (! clientWaiting) {
                val waitWorkflow = WaitWorkflow(
                    workflowName = workflowName,
                    workflowId = workflowId,
                    methodRunId = methodRunId,
                    clientName = clientName
                )
                launch { sendToWorkflowEngine(waitWorkflow) }
            }

            // wait for result
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

    override fun completeTask(taskName: TaskName, taskId: TaskId, value: Any?): CompletableFuture<Unit> = TODO("Not yet Implemented")

    override fun completeTask(taskName: TaskName, taskTag: TaskTag, value: Any?): CompletableFuture<Unit> = TODO("Not yet Implemented")

    override fun completeWorkflow(workflowName: WorkflowName, workflowId: WorkflowId, value: Any?): CompletableFuture<Unit> = TODO("Not yet Implemented")

    override fun completeWorkflow(workflowName: WorkflowName, workflowTag: WorkflowTag, value: Any?): CompletableFuture<Unit> = TODO("Not yet Implemented")

    override fun cancelTask(taskName: TaskName, taskId: TaskId): CompletableFuture<Unit> = scope.future {
        val msg = CancelTask(
            taskId = taskId,
            taskName = taskName
        )
        launch { sendToTaskEngine(msg) }

        Unit
    }

    override fun cancelTask(taskName: TaskName, taskTag: TaskTag): CompletableFuture<Unit> = scope.future {
        val msg = CancelTaskPerTag(
            taskTag = taskTag,
            taskName = taskName
        )
        launch { sendToTaskTagEngine(msg) }

        Unit
    }

    override fun cancelWorkflow(workflowName: WorkflowName, workflowId: WorkflowId): CompletableFuture<Unit> = scope.future {
        val msg = CancelWorkflow(
            workflowId = workflowId,
            workflowName = workflowName,
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
        launch { sendToWorkflowEngine(msg) }

        Unit
    }

    override fun cancelWorkflow(workflowName: WorkflowName, workflowTag: WorkflowTag): CompletableFuture<Unit> = scope.future {
        val msg = CancelWorkflowPerTag(
            workflowTag = workflowTag,
            workflowName = workflowName,
            reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
        )
        launch { sendToWorkflowTagEngine(msg) }

        Unit
    }

    override fun retryTask(taskName: TaskName, perTaskId: TaskId): CompletableFuture<Unit> = scope.future {
        val msg = RetryTask(
            taskId = perTaskId,
            taskName = taskName
        )
        launch { sendToTaskEngine(msg) }

        Unit
    }

    override fun retryTask(taskName: TaskName, perTaskTag: TaskTag): CompletableFuture<Unit> = scope.future {
        val msg = RetryTaskPerTag(
            taskTag = perTaskTag,
            taskName = taskName
        )
        launch { sendToTaskTagEngine(msg) }

        Unit
    }

    override fun retryWorkflow(workflowName: WorkflowName, workflowId: WorkflowId): CompletableFuture<Unit> = scope.future {
        val msg = RetryWorkflowTask(
            workflowId = workflowId,
            workflowName = workflowName
        )
        launch { sendToWorkflowEngine(msg) }

        Unit
    }

    override fun retryWorkflow(workflowName: WorkflowName, workflowTag: WorkflowTag): CompletableFuture<Unit> = scope.future {
        val msg = RetryWorkflowTaskPerTag(
            workflowTag = workflowTag,
            workflowName = workflowName
        )
        launch { sendToWorkflowTagEngine(msg) }

        Unit
    }

    // synchronously get task ids per tag
    override fun getTaskIdsPerTag(taskName: TaskName, taskTag: TaskTag): Set<UUID> {
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

        return taskIdsPerTag.taskIds.map { it.id }.toSet()
    }

    override fun getWorkflowIdsPerTag(workflowName: WorkflowName, workflowTag: WorkflowTag): Set<UUID> {
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

        return workflowIdsPerTag.workflowIds.map { it.id }.toSet()
    }

    private fun <R : Any?> dispatchNewTask(
        taskName: TaskName,
        method: Method,
        clientWaiting: Boolean,
        tags: Set<TaskTag>,
        options: TaskOptions,
        meta: TaskMeta
    ): Deferred<R> {
        val taskId = TaskId()
        // send messages asynchronously
        val future = scope.future {

            // add provided tags for this id
            tags.map {
                val addTaskTag = AddTaskTag(
                    taskTag = it,
                    taskName = taskName,
                    taskId = taskId
                )
                launch { sendToTaskTagEngine(addTaskTag) }
            }

            // dispatch this task
            val dispatchTask = DispatchTask(
                taskId = taskId,
                taskName = taskName,
                clientName = clientName,
                clientWaiting = clientWaiting,
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

            Unit
        }

        return DeferredTask(taskName, taskId, clientWaiting, this, future)
    }

    private fun <R : Any?> dispatchNewWorkflow(
        workflowName: WorkflowName,
        method: Method,
        clientWaiting: Boolean,
        tags: Set<WorkflowTag>,
        options: WorkflowOptions,
        meta: WorkflowMeta
    ): Deferred<R> {
        val workflowId = WorkflowId()
        val methodRunId = MethodRunId(workflowId.id)

        // send messages asynchronously
        val future = scope.future() {

            // add provided tags
            tags.map {
                val addWorkflowTag = AddWorkflowTag(
                    workflowTag = it,
                    workflowName = workflowName,
                    workflowId = workflowId
                )
                launch { sendToWorkflowTagEngine(addWorkflowTag) }
            }

            // dispatch workflow
            val dispatchWorkflow = DispatchWorkflow(
                workflowId = workflowId,
                workflowName = workflowName,
                clientName = clientName,
                clientWaiting = clientWaiting,
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

            Unit
        }

        return DeferredWorkflow(workflowName, workflowId, methodRunId, clientWaiting, this, future)
    }

    private fun <R : Any?> dispatchMethod(
        workflowName: WorkflowName,
        workflowId: WorkflowId,
        method: Method
    ): Deferred<R> {
        val methodRunId = MethodRunId()

        // send messages asynchronously
        val future = scope.future() {

            val dispatchMethodRun = DispatchMethodRun(
                workflowId = workflowId,
                workflowName = workflowName,
                methodRunId = methodRunId,
                clientName = clientName,
                parentWorkflowId = null,
                parentWorkflowName = null,
                parentMethodRunId = null,
                methodName = method.methodName,
                methodParameterTypes = method.methodParameterTypes,
                methodParameters = method.methodParameters
            )
            launch { sendToWorkflowEngine(dispatchMethodRun) }

            Unit
        }

        return DeferredWorkflow(workflowName, workflowId, methodRunId, false, this, future)
    }

    private fun <R : Any?> dispatchMethod(
        workflowName: WorkflowName,
        workflowTag: WorkflowTag,
        method: Method
    ): Deferred<R> {
        TODO("Not Yet Implemented")
    }

    override fun <R : Any?> send(
        workflowName: WorkflowName,
        channelName: ChannelName,
        workflowId: WorkflowId,
        signal: Any
    ): Deferred<R> {
        val channelSignalId = ChannelSignalId()
        // send messages asynchronously
        val future = scope.future() {
            val sendToChannel = SendToChannel(
                workflowId = workflowId,
                workflowName = workflowName,
                clientName = clientName,
                channelSignalId = channelSignalId,
                channelName = channelName,
                channelSignal = ChannelSignal.from(signal),
                channelSignalTypes = ChannelSignalType.allFrom(signal::class.java)
            )
            launch { sendToWorkflowEngine(sendToChannel) }

            Unit
        }

        return DeferredSend(channelSignalId, future)
    }

    override fun <R : Any?> send(
        workflowName: WorkflowName,
        channelName: ChannelName,
        workflowTag: WorkflowTag,
        signal: Any
    ): Deferred<R> {
        val channelSignalId = ChannelSignalId()
        // send messages asynchronously
        val future = scope.future() {
            val sendToChannelPerTag = SendToChannelPerTag(
                workflowTag = workflowTag,
                workflowName = workflowName,
                clientName = clientName,
                channelSignalId = channelSignalId,
                channelName = channelName,
                channelSignal = ChannelSignal.from(signal),
                channelSignalTypes = ChannelSignalType.allFrom(signal::class.java)
            )
            launch { sendToWorkflowTagEngine(sendToChannelPerTag) }

            Unit
        }

        return DeferredSend(channelSignalId, future)
    }
}
