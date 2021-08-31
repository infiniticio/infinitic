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
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskTag
import io.infinitic.common.tasks.engine.SendToTaskEngine
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.tasks.tags.SendToTaskTagEngine
import io.infinitic.common.tasks.tags.messages.AddTaskTag
import io.infinitic.common.tasks.tags.messages.GetTaskIds
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowTag
import io.infinitic.common.workflows.engine.SendToWorkflowEngine
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.tags.SendToWorkflowTagEngine
import io.infinitic.common.workflows.tags.messages.AddWorkflowTag
import io.infinitic.common.workflows.tags.messages.GetWorkflowIds
import io.infinitic.common.workflows.tags.messages.SendToChannelPerTag
import io.infinitic.exceptions.clients.AlreadyCompletedWorkflowException
import io.infinitic.exceptions.clients.CanceledDeferredException
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

    // synchronous call: task.method()
    override fun <T> dispatchAndWait(handler: TaskProxyHandler<*>): T = dispatch<T>(handler).await()

    // asynchronous call: async(newTask) { method() }
    internal fun <T> dispatch(handler: TaskProxyHandler<*>): DeferredTask<T> {
        checkMethodIsNotSuspend(handler.method)

        val taskId = TaskId()

        // store values
        val taskName = handler.taskName
        val isSync = handler.isSync
        val methodName = handler.methodName
        val method = handler.method
        val methodArgs = handler.methodArgs
        val taskTags = handler.taskTags!!
        val taskOptions = handler.taskOptions!!
        val taskMeta = handler.taskMeta!!

        // reset for reuse
        handler.reset()

        // handler now target an existing task
        handler.perTaskId = taskId

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

        return DeferredTask(taskName, taskId, isSync, this, future)
    }

    internal fun <T> await(deferredTask: DeferredTask<T>): T {
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
            is TaskCompleted -> taskResult.taskReturnValue.get() as T
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

    // synchronous call on a new workflow: newWorkflow.method()
    @Suppress("UNCHECKED_CAST")
    override fun <S> dispatchAndWait(handler: WorkflowProxyHandler<*>): S {
        // special case of getting a channel
        val method = handler.method

        if (method.returnType.kotlin.isSubclassOf(SendChannel::class)) {
            return SendChannelProxyHandler(
                method.returnType,
                handler.workflowName,
                ChannelName(method.name),
                handler.perWorkflowId,
                handler.perTag
            ) { this }.stub() as S
        }

        // dispatch and wait
        return dispatch<S>(handler).await()
    }

    // asynchronous workflow: async(newWorkflow) { method() }
    internal fun <T> dispatch(handler: WorkflowProxyHandler<*>): DeferredWorkflow<T> {
        checkMethodIsNotSuspend(handler.method)

        val workflowId = WorkflowId()

        // store values to use after handler reset
        val workflowName = handler.workflowName
        val isSync = handler.isSync

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
                methodParameters = MethodParameters.from(handler.method, handler.methodArgs),
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

        // handler now target an existing task
        handler.perWorkflowId = workflowId
        // reset isSync only
        handler.isSync = true

        return DeferredWorkflow(workflowName, workflowId, isSync, this, future)
    }

    internal fun <T> await(deferredWorkflow: DeferredWorkflow<T>): T {
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
            is WorkflowCompleted -> workflowResult.workflowReturnValue.get() as T
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

    // synchronous send on a channel: existingWorkflow.channel.send()
    override fun dispatchAndWait(handler: SendChannelProxyHandler<*>) = dispatch(handler)

    // asynchronous send on a channel: async(existingWorkflow.channel) { send() }
    private fun dispatch(handler: SendChannelProxyHandler<*>): CompletableFuture<Unit> {
        val method = handler.method

        if (method.name != SendChannel<*>::send.name) throw UnknownMethodInSendChannelException(
            "${handler.workflowName}",
            "${handler.channelName}",
            method.name
        )

        val event = handler.methodArgs[0]

        return scope.future() {
            when {
                handler.perTag != null -> {
                    val sendToChannelPerTag = SendToChannelPerTag(
                        workflowTag = handler.perTag!!,
                        workflowName = handler.workflowName,
                        clientName = clientName,
                        clientWaiting = handler.isSync,
                        channelEventId = ChannelEventId(),
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
                        channelEventId = ChannelEventId(),
                        channelName = handler.channelName,
                        channelEvent = ChannelEvent.from(event),
                        channelEventTypes = ChannelEventType.allFrom(event::class.java)
                    )
                    launch { sendToWorkflowEngine(sendToChannel) }
                }
                else -> thisShouldNotHappen()
            }

            Unit
        }
    }
}
