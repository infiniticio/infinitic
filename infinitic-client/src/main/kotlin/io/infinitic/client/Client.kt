// "Commons Clause" License Condition v1.0
//
// The Software is provided to you by the Licensor under the License, as defined
// below, subject to the following condition.
//
// Without limiting other conditions in the License, the grant of rights under the
// License will not include, and the License does not grant to you, the right to
// Sell the Software.
//
// For purposes of the foregoing, “Sell” means practicing any or all of the rights
// granted to you under the License to provide to third parties, for a fee or
// other consideration (including without limitation fees for hosting or
// consulting/ support services related to the Software), a product or service
// whose value derives, entirely or substantially, from the functionality of the
// Software. Any license notice or attribution required by the License must also
// include this Commons Clause License Condition notice.
//
// Software: Infinitic
//
// License: MIT License (https://opensource.org/licenses/MIT)
//
// Licensor: infinitic.io

package io.infinitic.client

import io.infinitic.common.tasks.data.MethodInput
import io.infinitic.common.tasks.data.MethodName
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.data.MethodOutput
import io.infinitic.common.tasks.data.MethodParameterTypes
import io.infinitic.common.tasks.exceptions.NoMethodCallAtDispatch
import io.infinitic.common.tasks.messages.CancelTask
import io.infinitic.common.tasks.messages.DispatchTask
import io.infinitic.common.tasks.messages.ForTaskEngineMessage
import io.infinitic.common.tasks.messages.RetryTask
import io.infinitic.common.tasks.proxies.MethodProxyHandler
import io.infinitic.common.workflows.Workflow
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.messages.DispatchWorkflow
import io.infinitic.common.workflows.messages.ForWorkflowEngineMessage
import io.infinitic.messaging.api.dispatcher.Emetter

class Client(
    private val taskEngineEmetter: Emetter<ForTaskEngineMessage>,
    private val workflowEngineEmetter: Emetter<ForWorkflowEngineMessage>
) {

    /*
    * Use this method to dispatch a workflow
    */
    suspend fun <T : Workflow> dispatch(
        workflowInterface: Class<T>,
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta(),
        apply: T.() -> Any?
    ): WorkflowInstance {
        // get a proxy for T
        val handler = MethodProxyHandler(workflowInterface)

        val klass = handler.instance()

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(workflowInterface.name, "dispatchWorkflow")

        val msg = DispatchWorkflow(
            workflowId = WorkflowId(),
            workflowName = WorkflowName.from(method),
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodInput = MethodInput.from(method, handler.args),
            workflowMeta = meta,
            workflowOptions = options
        )
        workflowEngineEmetter.send(msg)

        return WorkflowInstance(msg.workflowId)
    }

    /*
     * Use this method to dispatch a task
     */
    suspend fun <T : Any> dispatch(
        taskInterface: Class<T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        apply: T.() -> Any?
    ): TaskInstance {
        // get a proxy for T
        val handler = MethodProxyHandler(taskInterface)

        // get a proxy instance
        val klass = handler.instance()

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(taskInterface.name, "dispatchTask")

        val msg = DispatchTask(
            taskId = TaskId(),
            taskName = TaskName.from(method),
            methodName = MethodName.from(method),
            methodParameterTypes = MethodParameterTypes.from(method),
            methodInput = MethodInput.from(method, handler.args),
            taskOptions = options,
            taskMeta = meta
        )
        taskEngineEmetter.send(msg)

        return TaskInstance(msg.taskId)
    }

    /*
     * Use this method to manually retry a task
     * when a non-null parameter is provided, it will supersede current one
     */
    suspend fun retryTask(
        id: String,
        name: TaskName? = null,
        method: MethodName? = null,
        parameterTypes: MethodParameterTypes? = null,
        input: io.infinitic.common.tasks.data.MethodInput? = null,
        options: TaskOptions? = null,
        meta: TaskMeta? = null
    ) {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = name,
            methodName = method,
            methodParameterTypes = parameterTypes,
            methodInput = input,
            taskOptions = options,
            taskMeta = meta
        )
        taskEngineEmetter.send(msg)
    }

    /*
     * Use this method to manually cancel a task
     */
    suspend fun cancelTask(
        id: String,
        output: Any? = null
    ) {
        val msg = CancelTask(
            taskId = TaskId(id),
            taskOutput = MethodOutput(output)
        )
        taskEngineEmetter.send(msg)
    }
}
