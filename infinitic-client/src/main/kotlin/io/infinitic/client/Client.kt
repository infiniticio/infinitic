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
import io.infinitic.common.data.methods.MethodInput
import io.infinitic.common.data.methods.MethodName
import io.infinitic.common.data.methods.MethodOutput
import io.infinitic.common.data.methods.MethodParameterTypes
import io.infinitic.common.proxies.MethodProxyHandler
import io.infinitic.common.tasks.data.TaskId
import io.infinitic.common.tasks.data.TaskInstance
import io.infinitic.common.tasks.data.TaskMeta
import io.infinitic.common.tasks.data.TaskName
import io.infinitic.common.tasks.data.TaskOptions
import io.infinitic.common.tasks.engine.messages.CancelTask
import io.infinitic.common.tasks.engine.messages.DispatchTask
import io.infinitic.common.tasks.engine.messages.RetryTask
import io.infinitic.common.tasks.exceptions.NoMethodCallAtDispatch
import io.infinitic.common.workflows.data.workflows.WorkflowId
import io.infinitic.common.workflows.data.workflows.WorkflowInstance
import io.infinitic.common.workflows.data.workflows.WorkflowMeta
import io.infinitic.common.workflows.data.workflows.WorkflowName
import io.infinitic.common.workflows.data.workflows.WorkflowOptions
import io.infinitic.common.workflows.engine.messages.DispatchWorkflow
import io.infinitic.common.workflows.executors.Workflow

class Client(
    private val clientOutput: ClientOutput
) {

    /*
    Use this method to dispatch a workflow
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
        clientOutput.sendToWorkflowEngine(msg, 0F)

        return WorkflowInstance(msg.workflowId)
    }

    /*
    Use this method to dispatch a workflow
    */
    suspend inline fun <reified T : Workflow> dispatch(
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta(),
        noinline apply: T.() -> Any?
    ) = dispatch(T::class.java, options, meta, apply)

    /*
     Use this method to dispatch a task
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
            workflowId = null,
            methodRunId = null,
            taskOptions = options,
            taskMeta = meta
        )
        clientOutput.sendToTaskEngine(msg, 0F)

        return TaskInstance(msg.taskId)
    }

    /*
    Use this method to dispatch a task
    */
    suspend inline fun <reified T : Any> dispatch(
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        noinline apply: T.() -> Any?
    ) = dispatch(T::class.java, options, meta, apply)

    /*
     * Use this method to manually retry a task
     * when a non-null parameter is provided, it will supersede current one
     */
    suspend fun retryTask(
        id: String,
        name: TaskName? = null,
        methodName: MethodName? = null,
        methodParameterTypes: MethodParameterTypes? = null,
        methodInput: MethodInput? = null,
        taskOptions: TaskOptions? = null,
        taskMeta: TaskMeta? = null
    ) {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = name,
            methodName = methodName,
            methodParameterTypes = methodParameterTypes,
            methodInput = methodInput,
            taskOptions = taskOptions,
            taskMeta = taskMeta
        )
        clientOutput.sendToTaskEngine(msg, 0F)
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
            taskOutput = MethodOutput.from(output)
        )
        clientOutput.sendToTaskEngine(msg, 0F)
    }
}
