package io.infinitic.worker.workflowTask.commands

import io.infinitic.worker.workflowTask.WorkflowTaskContext
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class CommandProxy(private val workflowTaskContext: () -> WorkflowTaskContext) : InvocationHandler {

    /*
     * implements the synchronous processing of a task or child workflow
     */
    override fun invoke(proxy: Any?, method: Method, args: Array<out Any>?): Any? =
        workflowTaskContext()
            .dispatch(method, args ?: arrayOf(), method.returnType)
            .result()

    /*
     * provides a proxy instance of type T
     */
    inline fun <reified S> instance() = Proxy.newProxyInstance(
        S::class.java.classLoader,
        arrayOf(S::class.java),
        this
    ) as S
}
