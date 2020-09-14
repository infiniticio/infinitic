package io.infinitic.workflowManager.client

import io.infinitic.taskManager.common.proxies.MethodProxyHandler
import io.infinitic.taskManager.common.exceptions.NoMethodCallAtDispatch
import io.infinitic.workflowManager.common.data.workflows.WorkflowInstance
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.methodRuns.MethodInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMeta
import io.infinitic.workflowManager.common.data.methodRuns.MethodName
import io.infinitic.workflowManager.common.data.workflows.WorkflowName
import io.infinitic.workflowManager.common.data.workflows.WorkflowOptions
import io.infinitic.workflowManager.common.messages.DispatchWorkflow
import java.lang.reflect.Proxy
import io.infinitic.taskManager.client.Client as TaskClient

class Client() : TaskClient() {
    lateinit var workflowDispatcher: WorkflowDispatcher

    /*
     * Use this method to provide an actual implementation of AvroWorkflowDispatcher
     */
    fun setWorkflowDispatcher(avroDispatcher: AvroWorkflowDispatcher) {
        workflowDispatcher = WorkflowDispatcher(avroDispatcher)
    }

    /*
     * Use this method to dispatch a task
     * TODO: using class instance instead of interface is not supported
     */
    inline fun <reified T> dispatchWorkflow(
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
        workflowDispatcher.toWorkflowEngine(msg)

        return WorkflowInstance(msg.workflowId)
    }
}
