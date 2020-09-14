package io.infinitic.workflowManager.worker

import io.infinitic.taskManager.common.proxies.MethodProxyHandler
import io.infinitic.workflowManager.common.exceptions.NoMethodCallAtAsync
import io.infinitic.workflowManager.common.exceptions.WorkflowTaskContextNotInitialized
import io.infinitic.workflowManager.worker.deferred.Deferred
import io.infinitic.workflowManager.worker.commands.TaskProxyHandler
import io.infinitic.workflowManager.worker.data.MethodRunContext
import io.infinitic.workflowManager.worker.exceptions.KnownStepException
import io.infinitic.workflowManager.worker.exceptions.NewStepException

abstract class Workflow {
    var methodRunContext: MethodRunContext? = null
        get() = field ?: throw WorkflowTaskContextNotInitialized(this::class.java.name, MethodRunContext::class.java.name)

    /*
     * Use this method to proxy task or child workflows
     */
    protected inline fun <reified T : Any> proxy() = TaskProxyHandler { methodRunContext!! }.instance<T>()

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
    fun <S> async(branch: () -> S) = methodRunContext!!.async { branch() }
}
