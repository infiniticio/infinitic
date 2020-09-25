package io.infinitic.workflowManager.client

import io.infinitic.messaging.api.dispatcher.Dispatcher
import io.infinitic.common.taskManager.proxies.MethodProxyHandler
import io.infinitic.common.taskManager.exceptions.NoMethodCallAtDispatch
import io.infinitic.common.workflowManager.data.workflows.WorkflowInstance
import io.infinitic.common.workflowManager.data.workflows.WorkflowId
import io.infinitic.common.workflowManager.data.methodRuns.MethodInput
import io.infinitic.common.workflowManager.data.workflows.WorkflowMeta
import io.infinitic.common.workflowManager.data.methodRuns.MethodName
import io.infinitic.common.workflowManager.data.workflows.WorkflowName
import io.infinitic.common.workflowManager.data.workflows.WorkflowOptions
import io.infinitic.common.workflowManager.messages.DispatchWorkflow
import java.lang.reflect.Proxy
import io.infinitic.client.taskManager.Client as TaskClient

class Client(
    override val dispatcher: Dispatcher
) : TaskClient(dispatcher) {

    /*
     * Use this method to dispatch a task
     * TODO: using class instance instead of interface is not supported
     */
    suspend inline fun <reified T> dispatchWorkflow(
        options: WorkflowOptions = WorkflowOptions(),
        meta: WorkflowMeta = WorkflowMeta(),
        apply: T.() -> Any?
    ): WorkflowInstance {
        // get a proxy for T
        val handler = MethodProxyHandler()

        val klass = Proxy.newProxyInstance(
            T::class.java.classLoader,
            kotlin.arrayOf(T::class.java),
            handler
        ) as T

        // method call will actually be done through the proxy by handler
        klass.apply()

        // dispatch the workflow
        val method = handler.method ?: throw NoMethodCallAtDispatch(T::class.java.name, "dispatchWorkflow")

        val msg = DispatchWorkflow(
            workflowId = WorkflowId(),
            workflowName = WorkflowName(T::class.java.name),
            methodName = MethodName.from(method),
            methodInput = MethodInput.from(method, handler.args),
            workflowMeta = meta,
            workflowOptions = options
        )
        dispatcher.toWorkflowEngine(msg)

        return WorkflowInstance(msg.workflowId)
    }
}
