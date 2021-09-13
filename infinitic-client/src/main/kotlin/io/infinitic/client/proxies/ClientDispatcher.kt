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

package io.infinitic.client.proxies

import io.infinitic.client.Deferred
import io.infinitic.client.deferred.DeferredChannel
import io.infinitic.client.deferred.DeferredSendChannel
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
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.proxies.Dispatcher
import io.infinitic.common.proxies.NewProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.ProxyHandler
import io.infinitic.common.proxies.RunningProxyHandler
import io.infinitic.common.proxies.RunningTaskProxyHandler
import io.infinitic.common.proxies.RunningWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
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
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.workflows.WorkflowCancellationReason
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.CancelWorkflow
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
import io.infinitic.exceptions.clients.AlreadyCompletedWorkflowException
import io.infinitic.exceptions.clients.CanNotAwaitStubPerTag
import io.infinitic.exceptions.clients.CanceledDeferredException
import io.infinitic.exceptions.clients.ChannelUsedOnNewWorkflowException
import io.infinitic.exceptions.clients.FailedDeferredException
import io.infinitic.exceptions.clients.UnknownMethodInSendChannelException
import io.infinitic.exceptions.clients.UnknownTaskException
import io.infinitic.exceptions.clients.UnknownWorkflowException
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
import kotlin.reflect.full.isSubclassOf

internal class ClientDispatcher(
    val scope: CoroutineScope,
    val clientName: ClientName,
    val sendToTaskTagEngine: SendToTaskTagEngine,
    val sendToTaskEngine: SendToTaskEngine,
    val sendToWorkflowTagEngine: SendToWorkflowTagEngine,
    val sendToWorkflowEngine: SendToWorkflowEngine
) : Dispatcher {
    private val logger = KotlinLogging.logger {}

    private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 0)

    suspend fun handle(message: ClientMessage) {
        responseFlow.emit(message)
    }

    // synchronously get task ids per tag
    internal fun getTaskIdsPerTag(taskName: TaskName, taskTag: TaskTag): Set<UUID> {
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
    internal fun getWorkflowIdsPerTag(workflowName: WorkflowName, workflowTag: WorkflowTag): Set<UUID> {
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

    // synchronous call: stub.method()
    override fun <R : Any?> dispatchAndWait(handler: ProxyHandler<*>): R = dispatch<R>(handler, true).await()

    // asynchronous call: async(stub::method)(*args)
    internal fun <R : Any?> dispatch(handler: ProxyHandler<*>, isSync: Boolean): Deferred<R> = when (handler) {
        is NewProxyHandler -> dispatchNew(handler, isSync)
        is RunningProxyHandler -> dispatchRunning(handler, isSync)
    }

    // asynchronous call: async(newStub::method)(*args)
    private fun <R : Any?> dispatchNew(handler: NewProxyHandler<*>, isSync: Boolean): Deferred<R> {
        val method = handler.method
        checkMethodIsNotSuspend(method)

        return when (handler) {
            is NewTaskProxyHandler -> dispatchNewTask(handler, isSync)
            is NewWorkflowProxyHandler -> dispatchNewWorkflow(handler, isSync)
        }
    }

    // asynchronous call: async(existingStub::method)(*args)
    private fun <R : Any?> dispatchRunning(handler: RunningProxyHandler<*>, isSync: Boolean): Deferred<R> {
        val method = handler.method
        checkMethodIsNotSuspend(method)

        return when (handler) {
            is RunningTaskProxyHandler -> throw Exception("can not dispatch a method on running task")
            is RunningWorkflowProxyHandler -> {
                when (method.returnType.kotlin.isSubclassOf(SendChannel::class)) {
                    // special case of getting a channel from a workflow
                    true -> {
                        val channel = SendChannelProxyHandler(method.returnType as Class<out SendChannel<*>>, handler).stub()

                        DeferredChannel(handler, channel) as Deferred<R>
                    }
                    false -> TODO("Not Yet Implemented")
                }
            }
            is SendChannelProxyHandler -> dispatchSendChannel(handler)
        }
    }

    // asynchronous call: async(task::method)(*args)
    private fun <R : Any?> dispatchNewTask(handler: NewTaskProxyHandler<*>, isSync: Boolean = false): Deferred<R> {
        val taskId = TaskId()

        val runningTaskProxyHandler = handler.runningTaskProxyHandler(taskId)

        // store values
        val taskName = handler.taskName
        val methodName = handler.methodName
        val method = handler.method
        val methodArgs = handler.args
        val taskTags = handler.taskTags!!
        val taskOptions = handler.taskOptions!!
        val taskMeta = handler.taskMeta!!

        // send messages asynchronously
        val future = scope.future {

            // add provided tags for this id
            handler.taskTags!!.map {
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
                clientName = clientName,
                clientWaiting = isSync,
                taskName = taskName,
                methodName = MethodName(methodName),
                methodParameterTypes = MethodParameterTypes.from(method),
                methodParameters = MethodParameters.from(method, methodArgs),
                workflowId = null,
                workflowName = null,
                methodRunId = null,
                taskTags = taskTags,
                taskOptions = taskOptions,
                taskMeta = taskMeta
            )
            launch { sendToTaskEngine(dispatchTask) }

            Unit
        }

        return DeferredTask(runningTaskProxyHandler, isSync, this, future)
    }

    // asynchronous call: async(workflow::method)(*args)
    private fun <R : Any?> dispatchNewWorkflow(handler: NewWorkflowProxyHandler<*>, isSync: Boolean = false): Deferred<R> {
        val workflowId = WorkflowId()

        val runningWorkflowProxyHandler = handler.runningWorkflowProxyHandler(workflowId)

        // store values to use after handler reset
        val workflowName = handler.workflowName

        // send messages asynchronously
        val future = scope.future() {

            // add provided tags
            handler.workflowTags!!.map {
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
                clientName = clientName,
                clientWaiting = isSync,
                workflowName = workflowName,
                methodName = MethodName(handler.methodName),
                methodParameterTypes = MethodParameterTypes.from(handler.method),
                methodParameters = MethodParameters.from(handler.method, handler.args),
                parentWorkflowId = null,
                parentWorkflowName = null,
                parentMethodRunId = null,
                workflowTags = handler.workflowTags!!,
                workflowMeta = handler.workflowMeta!!,
                workflowOptions = handler.workflowOptions!!
            )
            launch { sendToWorkflowEngine(dispatchWorkflow) }

            Unit
        }

        return DeferredWorkflow(runningWorkflowProxyHandler, isSync, this, future)
    }

    // asynchronous send on a channel: async(existingWorkflow.channel) { send() }
    private fun <R : Any?> dispatchSendChannel(handler: SendChannelProxyHandler<*>): DeferredSendChannel<R> {
        val method = handler.method

        if (method.name != SendChannel<*>::send.name) throw UnknownMethodInSendChannelException(
            "${handler.workflowName}",
            "${handler.channelName}",
            method.name
        )

        val channelEventId = ChannelEventId()
        val event = handler.args[0]

        val future = scope.future() {
            when {
                handler.perTag != null -> {
                    val sendToChannelPerTag = SendToChannelPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName,
                        clientName = clientName,
                        channelEventId = channelEventId,
                        channelName = handler.channelName,
                        channelEvent = ChannelEvent.from(event),
                        channelEventTypes = ChannelEventType.allFrom(event::class.java)
                    )
                    launch { sendToWorkflowTagEngine(sendToChannelPerTag) }
                }
                handler.perWorkflowId != null -> {
                    val sendToChannel = SendToChannel(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = handler.workflowName,
                        clientName = clientName,
                        channelEventId = channelEventId,
                        channelName = handler.channelName,
                        channelEvent = ChannelEvent.from(event),
                        channelEventTypes = ChannelEventType.allFrom(event::class.java)
                    )
                    launch { sendToWorkflowEngine(sendToChannel) }
                }
                else -> throw ChannelUsedOnNewWorkflowException("${handler.workflowName}")
            }

            Unit
        }

        return DeferredSendChannel(handler.instance as RunningWorkflowProxyHandler<*>, channelEventId, future)
    }

    internal fun <R : Any?> await(deferredTask: DeferredTask<R>): R {
        val taskResult = scope.future {
            // if task was initially not sync, then send WaitTask message
            if (! deferredTask.isSync) {
                val waitTask = WaitTask(
                    taskId = deferredTask.taskId,
                    taskName = deferredTask.taskName,
                    clientName = clientName
                )
                launch { sendToTaskEngine(waitTask) }
            }
            // wait for result
            responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                it is TaskMessage && it.taskId == deferredTask.taskId
            }
        }.join()

        @Suppress("UNCHECKED_CAST")
        return when (taskResult) {
            is TaskCompleted -> taskResult.taskReturnValue.get() as R
            is TaskCanceled -> throw CanceledDeferredException(
                "${deferredTask.taskId}",
                "${deferredTask.taskName}",
            )
            is TaskFailed -> throw FailedDeferredException(
                "${deferredTask.taskId}",
                "${deferredTask.taskName}",
                taskResult.error
            )
            is UnknownTask -> throw UnknownTaskException(
                "${deferredTask.taskId}",
                "${deferredTask.taskName}"
            )
            else -> throw RuntimeException("Unexpected ${taskResult::class}")
        }
    }

    internal fun <R : Any?> await(deferredWorkflow: DeferredWorkflow<R>): R {
        val workflowResult = scope.future {
            // if task was not initially sync, then send WaitTask message
            if (! deferredWorkflow.isSync) {
                val waitWorkflow = WaitWorkflow(
                    workflowId = deferredWorkflow.workflowId,
                    workflowName = deferredWorkflow.workflowName,
                    clientName = clientName
                )
                launch { sendToWorkflowEngine(waitWorkflow) }
            }

            // wait for result
            responseFlow.first {
                logger.debug { "ResponseFlow: $it" }
                (it is WorkflowMessage && it.workflowId == deferredWorkflow.workflowId)
            }
        }.join()

        @Suppress("UNCHECKED_CAST")
        return when (workflowResult) {
            is WorkflowCompleted -> workflowResult.workflowReturnValue.get() as R
            is WorkflowCanceled -> throw CanceledDeferredException(
                "${deferredWorkflow.workflowId}",
                "${deferredWorkflow.workflowName}"
            )
            is WorkflowFailed -> throw FailedDeferredException(
                "${deferredWorkflow.workflowId}",
                "${deferredWorkflow.workflowName}",
                workflowResult.error
            )
            is UnknownWorkflow -> throw UnknownWorkflowException(
                "${deferredWorkflow.workflowId}",
                "${deferredWorkflow.workflowName}"
            )
            is WorkflowAlreadyCompleted -> throw AlreadyCompletedWorkflowException(
                "${deferredWorkflow.workflowId}",
                "${deferredWorkflow.workflowName}"
            )
            else -> throw RuntimeException("Unexpected ${workflowResult::class}")
        }
    }

    internal fun <T : Any> cancelTask(handler: RunningTaskProxyHandler<T>): CompletableFuture<Unit> {

        return scope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = CancelTask(
                        taskId = handler.perTaskId!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = CancelTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    internal fun <T : Any> cancelWorkflow(handler: RunningWorkflowProxyHandler<T>): CompletableFuture<Unit> {

        return scope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = CancelWorkflow(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = handler.workflowName,
                        reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                    )
                    launch { sendToWorkflowEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = CancelWorkflowPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName,
                        reason = WorkflowCancellationReason.CANCELED_BY_CLIENT
                    )
                    launch { sendToWorkflowTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    internal fun <T : Any> retryTask(handler: RunningTaskProxyHandler<T>): CompletableFuture<Unit> {

        return scope.future {
            when {
                handler.perTaskId != null -> {
                    val msg = RetryTask(
                        taskId = handler.perTaskId!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = RetryTaskPerTag(
                        taskTag = handler.perTag!!,
                        taskName = handler.taskName
                    )
                    launch { sendToTaskTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    internal fun <T : Any> retryWorkflow(handler: RunningWorkflowProxyHandler<T>): CompletableFuture<Unit> {

        return scope.future {
            when {
                handler.perWorkflowId != null -> {
                    val msg = RetryWorkflowTask(
                        workflowId = handler.perWorkflowId!!,
                        workflowName = handler.workflowName
                    )
                    launch { sendToWorkflowEngine(msg) }
                }
                handler.perTag != null -> {
                    val msg = RetryWorkflowTaskPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName
                    )
                    launch { sendToWorkflowTagEngine(msg) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }

    internal fun <R : Any?> awaitTask(handler: RunningTaskProxyHandler<*>): R = when {
        handler.perTaskId != null -> DeferredTask<R>(
            handler,
            isSync = false,
            dispatcher = this
        ).await()
        handler.perTag != null -> throw CanNotAwaitStubPerTag("${handler.taskName}")
        else -> thisShouldNotHappen()
    }

    internal fun <R : Any?> awaitWorkflow(handler: RunningWorkflowProxyHandler<*>): R = when {
        handler.perWorkflowId != null -> DeferredWorkflow<R>(
            handler,
            isSync = false,
            dispatcher = this
        ).await()
        handler.perTag != null -> throw CanNotAwaitStubPerTag("${handler.workflowName}")
        else -> thisShouldNotHappen()
    }
}
