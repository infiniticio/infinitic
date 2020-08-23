package io.infinitic.workflowManager.client

import io.infinitic.taskManager.client.ProxyHandler
import io.infinitic.taskManager.common.exceptions.NoMethodCallAtDispatch
import io.infinitic.workflowManager.common.data.Workflow
import io.infinitic.workflowManager.common.data.workflows.WorkflowId
import io.infinitic.workflowManager.common.data.workflows.WorkflowInput
import io.infinitic.workflowManager.common.data.workflows.WorkflowMeta
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
    fun setDispatcher(avroDispatcher: AvroWorkflowDispatcher) {
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
    ): Workflow {
        // get a proxy for T
        val handler = ProxyHandler()

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
            workflowName = WorkflowName.from(method),
            workflowInput = WorkflowInput.from(method, handler.args),
            workflowMeta = meta.withParametersTypesFrom(method),
            workflowOptions = options
        )
        workflowDispatcher.toWorkflowEngine(msg)

        return Workflow(msg.workflowId)
    }
}
