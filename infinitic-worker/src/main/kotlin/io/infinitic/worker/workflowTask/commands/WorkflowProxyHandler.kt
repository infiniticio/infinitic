package io.infinitic.worker.workflowTask.commands

import io.infinitic.common.workflowManager.Workflow
import io.infinitic.worker.workflowTask.WorkflowTaskContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

internal class WorkflowProxyHandler<T : Workflow>(
    private val klass: Class<T>,
    private val workflowTaskContext: () -> WorkflowTaskContext
) : InvocationHandler {
    /*
     * implements the synchronous processing of a task or child workflow
     */
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? {
        if (method.name == "toString") return klass.name

        return workflowTaskContext()
            .dispatchWorkflow<T>(method, args ?: arrayOf())
            .result()
    }

    /*
     * provides a proxy instance of type T
     */
    @Suppress("UNCHECKED_CAST")
    fun instance(): T = Proxy.newProxyInstance(
        klass.classLoader,
        arrayOf(klass),
        this
    ) as T
}
