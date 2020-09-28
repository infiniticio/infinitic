package io.infinitic.client

import io.infinitic.common.taskManager.Task
import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.data.TaskInstance
import io.infinitic.common.taskManager.data.TaskId
import io.infinitic.common.taskManager.data.TaskInput
import io.infinitic.common.taskManager.data.TaskMeta
import io.infinitic.common.taskManager.data.TaskName
import io.infinitic.common.taskManager.data.TaskOptions
import io.infinitic.common.taskManager.data.TaskOutput
import io.infinitic.common.taskManager.exceptions.NoMethodCallAtDispatch
import io.infinitic.common.taskManager.messages.CancelTask
import io.infinitic.common.taskManager.messages.DispatchTask
import io.infinitic.common.taskManager.messages.RetryTask
import io.infinitic.common.taskManager.proxies.MethodProxyHandler
import io.infinitic.common.workflowManager.Workflow
import io.infinitic.common.workflowManager.data.methodRuns.MethodInput
import io.infinitic.common.workflowManager.data.methodRuns.MethodName
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.workflows.WorkflowInstance
import io.infinitic.common.workflowManager.data.workflows.WorkflowMeta
import io.infinitic.common.workflowManager.data.workflows.WorkflowName
import io.infinitic.common.workflowManager.data.workflows.WorkflowOptions
import io.infinitic.common.workflowManager.messages.DispatchWorkflow
import java.lang.reflect.Proxy

class Client(val dispatcher: Dispatcher) {


    /*
    * Use this method to dispatch a workflow
    */
    suspend fun <T: Workflow> dispatch(
        workflowInterface: Class<T>,
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta(),
        apply: T.() -> Any?
    ): WorkflowInstance {
        // get a proxy for T
        val handler = MethodProxyHandler()

        val klass = handler.instance(workflowInterface)

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(workflowInterface.name, "dispatchWorkflow")

        val msg = DispatchWorkflow(
            workflowId = WorkflowId(),
            workflowName = WorkflowName.from(method),
            methodName = MethodName.from(method),
            methodInput = MethodInput.from(method, handler.args),
            workflowMeta = meta,
            workflowOptions = options
        )
        dispatcher.toWorkflowEngine(msg)

        return WorkflowInstance(msg.workflowId)
    }

    /*
     * Use this method to dispatch a task
     */
    suspend fun <T: Task> dispatch(
        taskInterface: Class<T>,
        options: TaskOptions = TaskOptions(),
        meta: TaskMeta = TaskMeta(),
        apply: T.() -> Any?
    ): TaskInstance {
        // get a proxy for T
        val handler = MethodProxyHandler()

        // get a proxy instance
        val klass = handler.instance(taskInterface)

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(taskInterface.name, "dispatchTask")

        val msg = DispatchTask(
            taskId = TaskId(),
            taskName = TaskName.from(method),
            taskInput = TaskInput.from(method, handler.args),
            taskOptions = options,
            taskMeta = meta.withParametersTypesFrom(method)
        )
        dispatcher.toTaskEngine(msg)

        return TaskInstance(msg.taskId)
    }

    /*
     * Use this method to manually retry a task
     * when a non-null parameter is provided, it will supersede current one
     */
    suspend fun retryTask(
        id: String,
        name: TaskName? = null,
        input: TaskInput? = null,
        options: TaskOptions? = null,
        meta: TaskMeta? = null
    ) {
        val msg = RetryTask(
            taskId = TaskId(id),
            taskName = name,
            taskInput = input,
            taskOptions = options,
            taskMeta = meta
        )
        dispatcher.toTaskEngine(msg)
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
            taskOutput = TaskOutput(output)
        )
        dispatcher.toTaskEngine(msg)
    }
}
