package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.worker.data.WorkflowTaskContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyWorkflowHandler(val context: () -> WorkflowTaskContext) : InvocationHandler {
    private var method: Method? = null
    private lateinit var args: Array<out Any>

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        this.method = method
        this.args = args ?: arrayOf()

        val context = context()

        throw Exception("Workflows in workflows not yet implemented")
    }
}
