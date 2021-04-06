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
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.UnknownTaskWaited
import io.infinitic.common.clients.messages.UnknownWorkflowWaited
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.proxies.Dispatcher
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.proxies.TaskProxyHandler
import io.infinitic.common.proxies.WorkflowProxyHandler
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.AddWorkflowTag
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tags.transport.SendToTagEngine
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.TaskEngineMessage
import io.infinitic.common.tasks.engine.messages.WaitTask
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.engine.messages.SendToChannel
import io.infinitic.common.workflows.engine.messages.WaitWorkflow
import io.infinitic.common.workflows.engine.messages.WorkflowEngineMessage
import io.infinitic.exceptions.ChannelUsedOnNewWorkflow
import io.infinitic.exceptions.MultipleMethodCalls
import io.infinitic.exceptions.NoMethodCall
import io.infinitic.exceptions.NoSendMethodCall
import io.infinitic.exceptions.UnknownMethodInSendChannel
import io.infinitic.exceptions.UnknownTask
import io.infinitic.exceptions.UnknownWorkflow
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlin.reflect.full.isSubclassOf

internal class ClientDispatcher(
    val clientName: ClientName,
    val sendToTagEngine: SendToTagEngine,
    val sendToTaskEngine: (suspend (TaskEngineMessage) -> Unit),
    val sendToWorkflowEngine: (suspend (WorkflowEngineMessage) -> Unit)
) : Dispatcher {
    // could be replay = 0
    // but replay = 1 makes tests easier, as we can emit a message before listening
    private val responseFlow = MutableSharedFlow<ClientMessage>(replay = 1)

    suspend fun handle(message: ClientMessage) {
        responseFlow.emit(message)
    }

    // asynchronous call: async(newTask) { method() }
    internal fun <T> dispatch(handler: TaskProxyHandler<*>): DeferredTask<T> {
        val method = when (handler.methods.size) {
            0 -> throw NoMethodCall(handler.klass.name)
            1 -> handler.methods[0]
            else -> throw MultipleMethodCalls(
                handler.klass.name,
                handler.methods.first().name,
                handler.methods.last().name
            )
        }
        val args = handler.args[0]

        checkMethodIsNotSuspend(method)

        val taskId = TaskId()
        val taskName = TaskName.from(method)
        val isSync = handler.isSync

        // add provided tags for this id
        val addTaskTags = handler.tags!!.map {
            AddTaskTag(
                tag = it,
                name = taskName,
                taskId = taskId
            )
        }

        // dispatch this task
        val dispatchTask = DispatchTask(
            taskId = taskId,
            clientName = clientName,
            clientWaiting = handler.isSync,
            taskName = taskName,
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodParameters = MethodParameters.from(method, args),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            tags = handler.tags!!,
            taskOptions = handler.taskOptions!!,
            taskMeta = handler.taskMeta!!
        )

        // send messages
        GlobalScope.future {
            addTaskTags.map { sendToTagEngine(it) }
            sendToTaskEngine(dispatchTask)
        }.join()

        // reset for reuse
        handler.reset()

        // handler now target an existing task
        handler.perTaskId = taskId

        return DeferredTask<T>(taskName, taskId, isSync, this)
    }

    internal fun <T> await(deferredTask: DeferredTask<T>): T {
        // if task was not sync, then send WaitTask message
        if (! deferredTask.isSync) {
            val waitTask = WaitTask(
                taskId = deferredTask.taskId,
                taskName = deferredTask.taskName,
                clientName = clientName
            )
            GlobalScope.future {
                sendToTaskEngine(waitTask)
            }.join()
        }

        // wait for result
        val taskresult = runBlocking {
            responseFlow.first {
                (it is TaskCompleted && it.taskId == deferredTask.taskId) ||
                    (it is UnknownTaskWaited && it.taskId == deferredTask.taskId)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return when (taskresult) {
            is TaskCompleted -> taskresult.taskReturnValue.get() as T
            is UnknownTaskWaited -> throw UnknownTask(
                "${deferredTask.taskId}",
                "${deferredTask.taskName}"
            )
            else -> throw RuntimeException("This should not happen")
        }
    }

    // synchronous call: task.method()
    override fun <S> dispatchAndWait(handler: TaskProxyHandler<*>): S = dispatch<S>(handler).await()

    // asynchronous workflow: async(newWorkflow) { method() }
    internal fun <T> dispatch(handler: WorkflowProxyHandler<*>): DeferredWorkflow<T> {
        val method = when (handler.methods.size) {
            0 -> throw NoMethodCall(handler.klass.name)
            1 -> handler.methods[0]
            else -> throw MultipleMethodCalls(
                handler.klass.name,
                handler.methods.first().name,
                handler.methods.last().name
            )
        }
        val args = handler.args.last()

        checkMethodIsNotSuspend(method)

        val workflowId = WorkflowId()
        val workflowName = WorkflowName.from(method)
        val isSync = handler.isSync

        // add provided tags
        val addWorkflowTags = handler.tags!!.map {
            AddWorkflowTag(
                tag = it,
                name = workflowName,
                workflowId = workflowId
            )
        }

        // dispatch workflow
        val dispatchWorkflow = DispatchWorkflow(
            workflowId = workflowId,
            clientName = clientName,
            clientWaiting = isSync,
            workflowName = WorkflowName.from(method),
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodParameters = MethodParameters.from(method, args),
            parentWorkflowId = null,
            parentMethodRunId = null,
            tags = handler.tags!!,
            workflowMeta = handler.workflowMeta!!,
            workflowOptions = handler.workflowOptions!!
        )

        // send messages
        GlobalScope.future {
            addWorkflowTags.map { sendToTagEngine(it) }
            sendToWorkflowEngine(dispatchWorkflow)
        }.join()

        // handler now target an existing task
        handler.perWorkflowId = workflowId
        // reset isSync only
        handler.isSync = true

        return DeferredWorkflow(workflowName, workflowId, isSync, this)
    }

    internal fun <T> await(deferredWorkflow: DeferredWorkflow<T>): T {
        // if task was not sync, then send WaitTask message
        if (! deferredWorkflow.isSync) {
            val waitWorkflow = WaitWorkflow(
                workflowId = deferredWorkflow.workflowId,
                workflowName = deferredWorkflow.workflowName,
                clientName = clientName
            )
            GlobalScope.future {
                sendToWorkflowEngine(waitWorkflow)
            }.join()
        }

        // wait for result
        val result = runBlocking {
            responseFlow.first {
                (it is WorkflowCompleted && it.workflowId == deferredWorkflow.workflowId) ||
                    (it is UnknownWorkflowWaited && it.workflowId == deferredWorkflow.workflowId)
            }
        }

        @Suppress("UNCHECKED_CAST")
        return when (result) {
            is WorkflowCompleted -> result.workflowReturnValue.get() as T
            is UnknownWorkflowWaited -> throw UnknownWorkflow(
                "${deferredWorkflow.workflowId}",
                "${deferredWorkflow.workflowName}"
            )
            else -> throw RuntimeException("This should not happen")
        }
    }

    // synchronous call on a new workflow: newWorkflow.method()
    @Suppress("UNCHECKED_CAST")
    override fun <S> dispatchAndWait(handler: WorkflowProxyHandler<*>): S {
        // special case of getting a channel
        val method = handler.methods.last()
        if (method.returnType.kotlin.isSubclassOf(SendChannel::class)) {
            return when (handler.isNew()) {
                true -> throw ChannelUsedOnNewWorkflow(handler.klass.name)
                false -> SendChannelProxyHandler(
                    method.returnType,
                    WorkflowName(handler.klass.name),
                    ChannelName(method.name),
                    handler.perWorkflowId,
                    handler.perTag
                ) { this }.stub() as S
            }
        }

        // dispatch and wait
        return dispatch<S>(handler).await()
    }

    // asynchronous send on a channel: async(existingWorkflow.channel) { send() }
    private fun dispatch(handler: SendChannelProxyHandler<*>) {
        val method = handler.methods.lastOrNull()
            ?: throw NoSendMethodCall(handler.klass.name, "${handler.channelName}")

        if (method.name != SendChannel<*>::send.name) throw UnknownMethodInSendChannel(
            "${handler.workflowName}",
            "${handler.channelName}",
            method.name
        )

        val event = handler.args.last()[0]

        if (handler.perTag != null) {
            val msg = SendToChannelPerTag(
                tag = handler.perTag!!,
                name = handler.workflowName,
                clientName = clientName,
                clientWaiting = handler.isSync,
                channelEventId = ChannelEventId(),
                channelName = handler.channelName,
                channelEvent = ChannelEvent.from(event),
                channelEventTypes = ChannelEventType.allFrom(event::class.java)
            )

            GlobalScope.future { sendToTagEngine(msg) }.join()

            return
        }

        if (handler.perWorkflowId != null) {
            val msg = SendToChannel(
                workflowId = handler.perWorkflowId!!,
                workflowName = handler.workflowName,
                clientName = clientName,
                channelEventId = ChannelEventId(),
                channelName = handler.channelName,
                channelEvent = ChannelEvent.from(event),
                channelEventTypes = ChannelEventType.allFrom(event::class.java)
            )

            GlobalScope.future { sendToWorkflowEngine(msg) }.join()
        }
    }

    // synchronous send on a channel: existingWorkflow.channel.send()
    override fun dispatchAndWait(handler: SendChannelProxyHandler<*>) {
        // dispatch
        dispatch(handler)
    }
}
