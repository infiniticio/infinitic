package io.infinitic.workflowManager.worker

import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.data.WorkflowTaskContext
import java.lang.reflect.Proxy

abstract class Workflow {
    public lateinit var workflowTaskContext: WorkflowTaskContext

    protected inline fun <reified T> taskProxy(): T = Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        ProxyTaskHandler {
            if (! this::workflowTaskContext.isInitialized) throw WorkflowTaskContextNotInitialized(this::class.java.name, WorkflowTaskContext::class.java.name)

            workflowTaskContext
        }
    ) as T
}
