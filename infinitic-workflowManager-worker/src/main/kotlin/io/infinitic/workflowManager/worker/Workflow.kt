package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.proxies.MethodProxyHandler
import io.infinitic.workflowManager.common.exceptions.NoMethodCallAtAsync
import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.commands.TaskProxyHandler
import io.infinitic.workflowManager.worker.data.MethodExecutionContext

abstract class Workflow {
    var methodExecutionContext: MethodExecutionContext? = null
        get() = field ?: throw WorkflowTaskContextNotInitialized(this::class.java.name, MethodExecutionContext::class.java.name)

    /*
    * Use this method to proxy task or child workflows
    */
    protected inline fun <reified T : Any> proxy() = TaskProxyHandler { methodExecutionContext!! }.instance<T>()

    /*
     * Use this method to dispatch a task
    */
    inline fun <reified T : Any, reified S> async(
        proxy: T,
        method: T.() -> S
    ): Deferred<S> {
        // get a proxy for T
        val handler = MethodProxyHandler()

        // get a proxy instance
        val klass = handler.instance<T>()

        // this call will capture method and arguments
        klass.method()

        // retrieve method
        val method = handler.method ?: throw NoMethodCallAtAsync(T::class.java.name)

        // dispatch this request
        return methodExecutionContext!!.dispatch(method, handler.args, S::class.java)
    }
}
