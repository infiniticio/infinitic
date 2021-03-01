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

import io.infinitic.client.transport.ClientOutput
import io.infinitic.common.clients.messages.ClientResponseMessage
import io.infinitic.common.clients.messages.TaskCompleted
import io.infinitic.common.clients.messages.WorkflowCompleted
import io.infinitic.common.data.MillisDuration
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.data.methods.MethodParameters
import io.infinitic.common.proxies.Dispatcher
import io.infinitic.common.proxies.NewTaskProxyHandler
import io.infinitic.common.proxies.NewWorkflowProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.exceptions.NoMethodCall
import io.infinitic.common.tasks.exceptions.SuspendMethodNotSupported
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.future.future
import kotlinx.coroutines.runBlocking
import kotlin.reflect.jvm.kotlinFunction

internal class ClientDispatcher(private val clientOutput: ClientOutput) : Dispatcher {
    // should be replay = 0
    // but replay = 1 make tests much easier, as we can emit a message before listening
    private val responseFlow = MutableSharedFlow<ClientResponseMessage>(replay = 1)

    suspend fun handle(message: ClientResponseMessage) = responseFlow.emit(message)

    fun dispatchTask(handler: NewTaskProxyHandler<*>): String {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name, "async")
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupported(handler.klass.name, method.name)

        val msg = DispatchTask(
            taskId = TaskId(),
            clientName = clientOutput.clientName,
            clientWaiting = handler.isSync,
            taskName = TaskName.from(method),
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodParameters = MethodParameters.from(method, handler.args),
            workflowId = null,
            methodRunId = null,
            taskOptions = handler.taskOptions,
            taskMeta = handler.taskMeta
        )
        GlobalScope.future { clientOutput.sendToTaskEngine(msg, MillisDuration(0)) }.join()

        // reset for reuse
        handler.reset()

        return "${msg.taskId}"
    }

    override fun <S> dispatchTaskAndWaitResult(handler: NewTaskProxyHandler<*>): S {
        // dispatch
        val taskId = TaskId(dispatchTask(handler))

        // wait for result
        val taskCompleted = runBlocking {
            responseFlow.first {
                it is TaskCompleted && it.taskId == taskId
            }
        } as TaskCompleted

        @Suppress("UNCHECKED_CAST")
        return taskCompleted.taskReturnValue.get() as S
    }

    fun dispatchWorkflow(handler: NewWorkflowProxyHandler<*>): String {
        val method = handler.method ?: throw NoMethodCall(handler.klass.name, "async")
        if (method.kotlinFunction?.isSuspend == true) throw SuspendMethodNotSupported(handler.klass.name, method.name)

        val msg = DispatchWorkflow(
            workflowId = WorkflowId(),
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
        GlobalScope.future { clientOutput.sendToWorkflowEngine(msg, MillisDuration(0)) }.join()

        // reset for reuse
        handler.reset()

        return "${msg.workflowId}"
    }

    override fun <S> dispatchWorkflowAndWaitResult(handler: NewWorkflowProxyHandler<*>): S {
        // dispatch
        val workflowId = WorkflowId(dispatchWorkflow(handler))

        // wait for result
        val workflowCompleted = runBlocking {
            responseFlow.first {
                it is WorkflowCompleted && it.workflowId == workflowId
            }
        } as WorkflowCompleted

        @Suppress("UNCHECKED_CAST")
        return workflowCompleted.workflowReturnValue.get() as S
    }
}
