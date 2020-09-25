package io.infinitic.workflowManager.worker

import io.infinitic.common.taskManager.proxies.MethodProxyHandler
import io.infinitic.common.workflowManager.exceptions.NoMethodCallAtAsync
import io.infinitic.common.workflowManager.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.commands.CommandProxy
import io.infinitic.workflowManager.worker.data.MethodRunContext

abstract class Workflow {
    var methodRunContext: MethodRunContext? = null
        get() = field ?: throw WorkflowTaskContextNotInitialized(this::class.java.name, MethodRunContext::class.java.name)

    /*
     * Use this method to proxy a task or a child workflow
     */
    protected inline fun <reified T : Any> proxy() = CommandProxy { methodRunContext!! }.instance<T>()

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

        // dispatch this request
        return methodRunContext!!.dispatch(
            handler.method ?: throw NoMethodCallAtAsync(T::class.java.name),
            handler.args,
            S::class.java
        )
    }

    /*
     * Use this method to create an async branch
     */
    fun <S> async(branch: () -> S) = methodRunContext!!.async(branch)

    /*
     * Use this method to create an inline task
     */
    fun <S> task(inline: () -> S): S = methodRunContext!!.task(inline)
}
