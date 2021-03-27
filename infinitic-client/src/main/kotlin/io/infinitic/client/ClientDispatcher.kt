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

import io.infinitic.client.output.ClientOutput
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.messages.SendToChannelCompleted
import io.infinitic.common.clients.messages.SendToChannelFailed
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.proxies.Dispatcher
import io.infinitic.common.proxies.ExistingTaskProxyHandler
import io.infinitic.common.proxies.ExistingWorkflowProxyHandler
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.proxies.SendChannelProxyHandler
import io.infinitic.common.tags.data.Tag
import io.infinitic.common.tags.messages.AddTaskTag
import io.infinitic.common.tags.messages.AddWorkflowTag
import io.infinitic.common.tags.messages.SendToChannelPerTag
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.workflows.data.channels.ChannelEvent
import io.infinitic.common.workflows.data.channels.ChannelEventId
import io.infinitic.common.workflows.data.channels.ChannelEventType
import io.infinitic.common.workflows.data.channels.ChannelName
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.exceptions.ChannelUsedOnNewWorkflow
import io.infinitic.exceptions.IncorrectNewStub
import io.infinitic.exceptions.NoMethodCall
import io.infinitic.exceptions.NoSendMethodCall
import io.infinitic.exceptions.SuspendMethodNotSupported
import io.infinitic.exceptions.UnknownMethodInSendChannel
import io.infinitic.workflows.SendChannel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import java.lang.reflect.Method
import java.util.UUID
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.kotlinFunction
import io.infinitic.exceptions.SendToChannelFailed as SendToChannelFailedException

internal class ClientDispatcher(private val clientOutput: ClientOutput) : Dispatcher {
    // could be replay = 0
    // but replay = 1 makes tests easier, as we can emit a message before listening
    private val responseFlow = MutableSharedFlow<ClientResponseMessage>(replay = 100)

    suspend fun handle(message: ClientResponseMessage) {
        responseFlow.emit(message)
    }

    // asynchronous call on an existing task: async(existingTask) { method() }
    fun dispatch(handler: ExistingTaskProxyHandler<*>): UUID {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name)
        checkMethodIsNotSuspend(method)

        throw IncorrectNewStub(handler.klass.name, "dispatch")
    }

    // new asynchronous task: async(newTask) { method() }
    fun dispatch(handler: NewTaskProxyHandler<*>): UUID {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name)
        checkMethodIsNotSuspend(method)

        val taskId = TaskId()
        val taskName = TaskName.from(method)

        // add provided tags + id tag
        val tags = handler.taskOptions.tags.map { Tag(it) }.toSet().plus(Tag.of(taskId))
        val addWorkflowTags = tags.map {
            AddTaskTag(
                tag = it,
                name = taskName,
                taskId = taskId
            )
        }

        // dispatch workflow
        val dispatchTask = DispatchTask(
            taskId = taskId,
            clientName = clientOutput.clientName,
            clientWaiting = handler.isSync,
            taskName = taskName,
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodParameters = MethodParameters.from(method, handler.args),
            workflowId = null,
            workflowName = null,
            methodRunId = null,
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )

        // send messages
        GlobalScope.future {
            addWorkflowTags.map { clientOutput.sendToTagEngine(it) }
            clientOutput.sendToTaskEngine(dispatchTask)
        }.join()

        // reset for reuse
        handler.reset()

        return dispatchTask.taskId.id
    }

    // synchronous call on a new task: newTask.method()
    override fun <S> dispatchAndWait(handler: NewTaskProxyHandler<*>): S {
        // dispatch
        val taskId = TaskId(dispatch(handler))

        // wait for result
        val taskCompleted = runBlocking {
            responseFlow.first {
                it is TaskCompleted && it.taskId == taskId
            }
        } as TaskCompleted

        @Suppress("UNCHECKED_CAST")
        return taskCompleted.taskReturnValue.get() as S
    }

    // new asynchronous workflow: async(newWorkflow) { method() }
    fun dispatch(handler: NewWorkflowProxyHandler<*>): UUID {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name)
        checkMethodIsNotSuspend(method)

        val workflowId = WorkflowId()
        val workflowName = WorkflowName.from(method)

        // add provided tags + id tag
        val tags = handler.workflowOptions.tags.map { Tag(it) }.toSet().plus(Tag.of(workflowId))
        val addWorkflowTags = tags.map {
            AddWorkflowTag(
                tag = it,
                name = workflowName,
                workflowId = workflowId
            )
        }

        // dispatch workflow
        val dispatchWorkflow = DispatchWorkflow(
            workflowId = workflowId,
            clientName = clientOutput.clientName,
            clientWaiting = handler.isSync,
            workflowName = WorkflowName.from(method),
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodParameters = MethodParameters.from(method, handler.args),
            parentWorkflowId = null,
            parentMethodRunId = null,
            workflowMeta = handler.workflowMeta,
            workflowOptions = handler.workflowOptions
        )

        // send messages
        GlobalScope.future {
            addWorkflowTags.map { clientOutput.sendToTagEngine(it) }
            clientOutput.sendToWorkflowEngine(dispatchWorkflow)
        }.join()

        // reset for reuse
        handler.reset()

        return dispatchWorkflow.workflowId.id
    }

    // synchronous call on a new workflow: newWorkflow.method()
    override fun <S> dispatchAndWait(handler: NewWorkflowProxyHandler<*>): S {
        val method = handler.method!!
        checkMethodIsNotSuspend(method)

        // calling a channel from a new workflow does not make sense.
        if (method.returnType.kotlin.isSubclassOf(SendChannel::class)) {
            throw ChannelUsedOnNewWorkflow(handler.klass.name)
        }

        // dispatch
        val workflowId = WorkflowId(dispatch(handler))

        // wait for result
        val workflowCompleted = runBlocking {
            responseFlow.first {
                it is WorkflowCompleted && it.workflowId == workflowId
            }
        } as WorkflowCompleted

        @Suppress("UNCHECKED_CAST")
        return workflowCompleted.workflowReturnValue.get() as S
    }

    // asynchronous call on an existing workflow: async(existingWorkflow) { method() }
    fun dispatch(handler: ExistingWorkflowProxyHandler<*>): UUID {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name)
        checkMethodIsNotSuspend(method)

        throw IncorrectNewStub(handler.klass.name, "dispatch")
    }

    // synchronous call on a existing workflow: existingWorkflow.method()
    override fun <S> dispatchAndWait(handler: ExistingWorkflowProxyHandler<*>): S {
        val method = handler.method!!
        checkMethodIsNotSuspend(method)

        // case of channel, eg. myWorkflow.channel
        @Suppress("UNCHECKED_CAST")
        if (method.returnType.kotlin.isSubclassOf(SendChannel::class)) {
            val channel = SendChannelProxyHandler(
                method.returnType,
                handler.tag,
                WorkflowName(handler.klass.name),
                ChannelName(method.name)
            ) { this }.stub() as S
            // reset for reuse
            handler.reset()

            return channel
        }

        // synchronous method call on existing workflo is not yest implemented
        TODO("Not Yet Implemented")
    }

    // asynchronous send on a channel: async(existingWorkflow.channel) { send() }
    fun dispatch(handler: SendChannelProxyHandler<*>): UUID {
        val method = handler.method ?: throw NoSendMethodCall(handler.klass.name, "${handler.channelName}")
        checkMethodIsNotSuspend(method)

        if (method.name != SendChannel<*>::send.name) {
            throw UnknownMethodInSendChannel("${handler.workflowName}", "${handler.channelName}", method.name)
        }

        val msg = SendToChannelPerTag(
            tag = handler.tag,
            name = handler.workflowName,
            clientName = clientOutput.clientName,
            clientWaiting = handler.isSync,
            channelEventId = ChannelEventId(),
            channelName = handler.channelName,
            channelEvent = ChannelEvent.from(handler.args[0]),
            channelEventTypes = ChannelEventType.allFrom(handler.args[0]::class.java)
        )

        GlobalScope.future { clientOutput.sendToTagEngine(msg) }.join()

        // reset for reuse
        handler.reset()

        return msg.channelEventId.id
    }

    // synchronous send on a channel: existingWorkflow.channel.send()
    override fun dispatchAndWait(handler: SendChannelProxyHandler<*>) {
        // dispatch
        val sendId = ChannelEventId(dispatch(handler))

        // wait for response
        val response: ClientResponseMessage = runBlocking {
            responseFlow.first {
                (it is SendToChannelCompleted && it.channelEventId == sendId) ||
                    (it is SendToChannelFailed && it.channelEventId == sendId)
            }
        }
        when (response) {
            is SendToChannelCompleted -> Unit
            is SendToChannelFailed -> throw SendToChannelFailedException(
                "${handler.tag}",
                handler.klass.name,
                "$sendId"
            )
        }
    }

    private fun checkMethodIsNotSuspend(method: Method) {
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupported(method.declaringClass.name, method.name)
    }
}
