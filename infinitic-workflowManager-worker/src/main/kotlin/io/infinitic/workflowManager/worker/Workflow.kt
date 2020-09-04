package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.data.BranchContext
import java.lang.reflect.Proxy

abstract class Workflow {
    public lateinit var branchContext: BranchContext

    protected inline fun <reified T> proxy(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        ProxyHandler {
            if (! this::branchContext.isInitialized) throw WorkflowTaskContextNotInitialized(this::class.java.name, BranchContext::class.java.name)

            branchContext
        }
    ) as T
}
