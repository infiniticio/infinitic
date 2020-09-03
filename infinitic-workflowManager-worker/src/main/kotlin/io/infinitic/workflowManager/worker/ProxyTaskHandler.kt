package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.worker.data.WorkflowTaskContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method

class ProxyTaskHandler(val getWorkflowTaskContext: () -> WorkflowTaskContext) : InvocationHandler {
    private var method: Method? = null
    private lateinit var args: Array<out Any>

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
        this.method = method
        this.args = args ?: arrayOf()

        val workflowTaskContext = getWorkflowTaskContext()

        return null
    }

    fun test() {
        CoroutineScope(Dispatchers.Default).launch { }
    }
}
