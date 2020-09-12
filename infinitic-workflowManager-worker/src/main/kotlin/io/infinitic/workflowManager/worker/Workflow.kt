package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.proxies.MethodProxyHandler
import io.infinitic.workflowManager.common.exceptions.NoMethodCallAtAsync
import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.Deferred.Deferred
import io.infinitic.workflowManager.worker.data.MethodContext

abstract class Workflow {
    var methodContext: MethodContext? = null
        get() = field ?: throw WorkflowTaskContextNotInitialized(this::class.java.name, MethodContext::class.java.name)

    /*
    * Use this method to proxy task or child workflows
    */
    protected inline fun <reified T : Any> proxy() = TaskProxyHandler { methodContext!! }.instance<T>()

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
        return methodContext!!.dispatch(method, handler.args, S::class.java)
    }
}
