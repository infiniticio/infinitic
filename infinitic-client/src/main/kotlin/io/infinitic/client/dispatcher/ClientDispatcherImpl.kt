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
import io.infinitic.common.clients.messages.TaskCanceled
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.TaskFailed
import io.infinitic.common.clients.messages.TaskIdsPerTag
import io.infinitic.common.clients.messages.UnknownTask
import io.infinitic.common.clients.messages.UnknownWorkflow
import io.infinitic.common.clients.messages.WorkflowAlreadyCompleted
import io.infinitic.common.clients.messages.WorkflowCanceled
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.clients.messages.WorkflowFailed
import io.infinitic.common.clients.messages.WorkflowIdsPerTag
import io.infinitic.common.clients.messages.interfaces.TaskMessage
import io.infinitic.common.clients.messages.interfaces.WorkflowMessage
import io.infinitic.common.data.JobOptions
import io.infinitic.common.proxies.ChannelInstanceProxyHandler
import io.infinitic.common.proxies.ChannelSelectionProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.TaskInstanceProxyHandler
import io.infinitic.common.proxies.TaskSelectionProxyHandler
import io.infinitic.common.proxies.WorkflowInstanceProxyHandler
import io.infinitic.common.proxies.WorkflowSelectionProxyHandler
import io.infinitic.common.proxies.data.Method
import io.infinitic.common.proxies.data.Signal
import io.infinitic.common.proxies.data.TaskInstance
import io.infinitic.common.proxies.data.TaskSelection
import io.infinitic.common.proxies.data.WorkflowInstance
import io.infinitic.common.proxies.data.WorkflowSelection
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
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.DispatchWorkflowMethod
import io.infinitic.common.workflows.engine.messages.RetryWorkflowTask
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.CancelWorkflowPerTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.RetryWorkflowTaskPerTag
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.exceptions.clients.AlreadyCompletedWorkflowException
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.DispatchTaskSelectionException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.exceptions.clients.UnknownTaskException
import io.infinitic.exceptions.clients.UnknownWorkflowException
import io.infinitic.exceptions.clients.UseChannelOnNewWorkflowException
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
        is TaskInstanceProxyHandler ->
            dispatchNewTask(
                handler.instance(),
                handler.method(),
                clientWaiting,
                tags?.map { TaskTag(it) }?.toSet() ?: handler.taskTags,
                (options as TaskOptions?) ?: handler.taskOptions,
                meta?.run { TaskMeta(this) } ?: handler.taskMeta
            )
        is TaskSelectionProxyHandler ->
            throw DispatchTaskSelectionException(handler.klass.name, "dispatch")
        is WorkflowInstanceProxyHandler -> when (handler.isMethodChannel()) {
            // special case of getting a channel from a workflow
            true -> {
                @Suppress("UNCHECKED_CAST")
                val channel = ChannelInstanceProxyHandler<SendChannel<*>>(handler).stub()
                @Suppress("UNCHECKED_CAST")
                DeferredChannel(channel) as Deferred<R>
            }
            false -> dispatchNewWorkflow(
                handler.instance(),
                handler.method(),
                clientWaiting,
                tags?.map { WorkflowTag(it) }?.toSet() ?: handler.workflowTags,
                (options as WorkflowOptions?) ?: handler.workflowOptions,
                meta?.run { WorkflowMeta(this) } ?: handler.workflowMeta
            )
        }
        is WorkflowSelectionProxyHandler -> when (handler.isMethodChannel()) {
            // special case of getting a channel from a workflow
            true -> {
                @Suppress("UNCHECKED_CAST")
                val channel = ChannelSelectionProxyHandler<SendChannel<*>>(handler).stub()
                @Suppress("UNCHECKED_CAST")
                DeferredChannel(channel) as Deferred<R>
            }
            false -> dispatchWorkflowMethod(handler.selection(), handler.method(), clientWaiting)
        }
        is ChannelInstanceProxyHandler ->
            throw UseChannelOnNewWorkflowException(handler.workflowName.name)
        is ChannelSelectionProxyHandler ->
            send(handler.signal())
    }

    override fun <R : Any?> await(task: TaskSelection, clientWaiting: Boolean): R {
        val taskId = task.perTaskId ?: throw Exception("can not await per tag")

        val taskResult = scope.future {
            // if task was initially not sync, then send WaitTask message
            if (! clientWaiting) {
                val waitTask = WaitTask(
                    taskId = taskId,
                    taskName = task.taskName,
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
            is TaskCanceled -> throw CanceledDeferredException(
                "$taskId",
                "${task.taskName}",
            )
            is TaskFailed -> throw FailedDeferredException(
                "$taskId",
                "${task.taskName}",
                taskResult.error
            )
            is UnknownTask -> throw UnknownTaskException(
                "$taskId",
                "${task.taskName}"
            )
            else -> thisShouldNotHappen("Unexpected ${taskResult::class}")
        }
    }

    override fun <R : Any?> await(workflow: WorkflowSelection, clientWaiting: Boolean): R {
        val workflowId = workflow.perWorkflowId ?: throw Exception("can not await per tag")

        val workflowResult = scope.future {
            // if task was not initially sync, then send WaitTask message
            if (! clientWaiting) {
                val waitWorkflow = WaitWorkflow(
                    workflowId = workflowId,
                    workflowName = workflow.workflowName,
                    clientName = clientName
                )
                launch { sendToWorkflowEngine(waitWorkflow) }
            }

            // wait for result
            responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                (it is WorkflowMessage && it.workflowId == workflowId)
            }
        }.join()

        @Suppress("UNCHECKED_CAST")
        return when (workflowResult) {
            is WorkflowCompleted -> workflowResult.workflowReturnValue.get() as R
            is WorkflowCanceled -> throw CanceledDeferredException(
                "$workflowId",
                "${workflow.workflowName}"
            )
            is WorkflowFailed -> throw FailedDeferredException(
                "$workflowId",
                "${workflow.workflowName}",
                workflowResult.error
            )
            is UnknownWorkflow -> throw UnknownWorkflowException(
                "$workflowId",
                "${workflow.workflowName}"
            )
            is WorkflowAlreadyCompleted -> throw AlreadyCompletedWorkflowException(
                "$workflowId",
                "${workflow.workflowName}"
            )
            else -> thisShouldNotHappen("Unexpected ${workflowResult::class}")
        }
    }

    override fun cancelTask(task: TaskSelection): CompletableFuture<Unit> = scope.future {
        when {
            task.perTaskId != null -> {
                val msg = CancelTask(
                    taskId = task.perTaskId!!,
                    taskName = task.taskName
                )
                launch { sendToTaskEngine(msg) }
            }
            task.perTaskTag != null -> {
                val msg = CancelTaskPerTag(
                    taskTag = task.perTaskTag!!,
                    taskName = task.taskName
                )
                launch { sendToTaskTagEngine(msg) }
            }
            else -> thisShouldNotHappen()
        }

        Unit
    }

    override fun cancelWorkflow(workflow: WorkflowSelection): CompletableFuture<Unit> = scope.future {
        when {
            workflow.perWorkflowId != null -> {
                val msg = CancelWorkflow(
                    workflowId = workflow.perWorkflowId!!,
                    workflowName = workflow.workflowName,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                )
                launch { sendToWorkflowEngine(msg) }
            }
            workflow.perWorkflowTag != null -> {
                val msg = CancelWorkflowPerTag(
                    workflowTag = workflow.perWorkflowTag!!,
                    workflowName = workflow.workflowName,
                    reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                )
                launch { sendToWorkflowTagEngine(msg) }
            }
            else -> thisShouldNotHappen()
        }

        Unit
    }

    override fun retryTask(task: TaskSelection): CompletableFuture<Unit> = scope.future {
        when {
            task.perTaskId != null -> {
                val msg = RetryTask(
                    taskId = task.perTaskId!!,
                    taskName = task.taskName
                )
                launch { sendToTaskEngine(msg) }
            }
            task.perTaskTag != null -> {
                val msg = RetryTaskPerTag(
                    taskTag = task.perTaskTag!!,
                    taskName = task.taskName
                )
                launch { sendToTaskTagEngine(msg) }
            }
            else -> thisShouldNotHappen()
        }

        Unit
    }

    override fun retryWorkflow(workflow: WorkflowSelection): CompletableFuture<Unit> = scope.future {
        when {
            workflow.perWorkflowId != null -> {
                val msg = RetryWorkflowTask(
                    workflowId = workflow.perWorkflowId!!,
                    workflowName = workflow.workflowName
                )
                launch { sendToWorkflowEngine(msg) }
            }
            workflow.perWorkflowTag != null -> {
                val msg = RetryWorkflowTaskPerTag(
                    workflowTag = workflow.perWorkflowTag!!,
                    workflowName = workflow.workflowName
                )
                launch { sendToWorkflowTagEngine(msg) }
            }
            else -> thisShouldNotHappen()
        }

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

    // synchronously get workflow ids per tag
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

    // asynchronous call: async(task::method)(*args)
    private fun <R : Any?> dispatchNewTask(
        task: TaskInstance,
        method: Method,
        clientWaiting: Boolean,
        tags: Set<TaskTag>,
        options: TaskOptions,
        meta: TaskMeta
    ): Deferred<R> {
        // send messages asynchronously
        val future = scope.future {

            // add provided tags for this id
            tags.map {
                val addTaskTag = AddTaskTag(
                    taskTag = it,
                    taskName = task.taskName,
                    taskId = task.taskId!!
                )
                launch { sendToTaskTagEngine(addTaskTag) }
            }

            // dispatch this task
            val dispatchTask = DispatchTask(
                taskId = task.taskId!!,
                taskName = task.taskName,
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

        return DeferredTask(task.selection(), clientWaiting, this, future)
    }

    // asynchronous call: async(workflow::method)(*args)
    private fun <R : Any?> dispatchNewWorkflow(
        workflow: WorkflowInstance,
        method: Method,
        clientWaiting: Boolean,
        tags: Set<WorkflowTag>,
        options: WorkflowOptions,
        meta: WorkflowMeta
    ): Deferred<R> {
        // send messages asynchronously
        val future = scope.future() {

            // add provided tags
            tags.map {
                val addWorkflowTag = AddWorkflowTag(
                    workflowTag = it,
                    workflowName = workflow.workflowName,
                    workflowId = workflow.workflowId
                )
                launch { sendToWorkflowTagEngine(addWorkflowTag) }
            }

            // dispatch workflow
            val dispatchWorkflow = DispatchWorkflow(
                workflowId = workflow.workflowId,
                workflowName = workflow.workflowName,
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

        return DeferredWorkflow(workflow.selection(), clientWaiting, this, future)
    }

    // asynchronous call: async(workflow::method)(*args)
    private fun <R : Any?> dispatchWorkflowMethod(
        workflow: WorkflowSelection,
        method: Method,
        clientWaiting: Boolean
    ): Deferred<R> {
        // send messages asynchronously
        val future = scope.future() {

            when {
                workflow.perWorkflowId != null -> {
                    // dispatch workflow method
                    val dispatchWorkflowMethod = DispatchWorkflowMethod(
                        workflowId = workflow.perWorkflowId!!,
                        workflowName = workflow.workflowName,
                        clientName = clientName,
                        clientWaiting = clientWaiting,
                        parentWorkflowId = null,
                        parentWorkflowName = null,
                        parentMethodRunId = null,
                        methodName = method.methodName,
                        methodParameterTypes = method.methodParameterTypes,
                        methodParameters = method.methodParameters
                    )
                    launch { sendToWorkflowEngine(dispatchWorkflowMethod) }
                }
                workflow.perWorkflowTag != null -> {
                    TODO("NotYetImplemented")
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }

        return DeferredWorkflow(workflow, clientWaiting, this, future)
    }

    // asynchronous send a signal: existingWorkflow.channel.send()
    override fun <R : Any?> send(signal: Signal): DeferredSend<R> {
        // send messages asynchronously
        val future = scope.future() {
            when {
                signal.perWorkflowTag != null -> {
                    val sendToChannelPerTag = SendToChannelPerTag(
                        workflowTag = signal.perWorkflowTag!!,
                        workflowName = signal.workflowName,
                        clientName = clientName,
                        channelSignalId = signal.channelSignalId,
                        channelName = signal.channelName,
                        channelSignal = signal.channelSignal,
                        channelSignalTypes = signal.channelSignalTypes
                    )
                    launch { sendToWorkflowTagEngine(sendToChannelPerTag) }
                }
                signal.perWorkflowId != null -> {
                    val sendToChannel = SendToChannel(
                        workflowId = signal.perWorkflowId!!,
                        workflowName = signal.workflowName,
                        clientName = clientName,
                        channelSignalId = signal.channelSignalId,
                        channelName = signal.channelName,
                        channelSignal = signal.channelSignal,
                        channelSignalTypes = signal.channelSignalTypes
                    )
                    launch { sendToWorkflowEngine(sendToChannel) }
                }
                else -> throw UseChannelOnNewWorkflowException("${signal.workflowName}")
            }

            Unit
        }

        return DeferredSend(signal, future)
    }
}
